/*
 * Copyright (c) 2017 Michael Krotscheck
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.krotscheck.kangaroo.authz.oauth2.resource.authorize;

import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.AuthenticatorState;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.common.util.ValidationUtil;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * Implicit request handler.
 *
 * @author Michael Krotscheck
 */
public final class ImplicitHandler implements IAuthorizeHandler {

    /**
     * The system injector.
     */
    private final InjectionManager injector;

    /**
     * The active database session.
     */
    private final Session session;

    /**
     * Create a new handler.
     *
     * @param injector The system injection manager..
     * @param session  The hibernate session.
     */
    @Inject
    @SuppressWarnings({"CPD-START"})
    public ImplicitHandler(final InjectionManager injector,
                           final Session session) {
        this.injector = injector;
        this.session = session;
    }

    /**
     * Provided a stored intermediate authenticator state, attempt to resolve
     * an instance of the associated authenticator implementation.
     *
     * @param state The state to resolve.
     * @return An authenticator Impl, available from the injection context.
     */
    public IAuthenticator getAuthenticator(final AuthenticatorState state) {
        Authenticator a = state.getAuthenticator();
        return injector.getInstance(IAuthenticator.class, a.getType().name());
    }

    /**
     * Handle an initial authorization request using the implicit flow.
     * This handler has two distinct modes of operation - the first is that
     * of an unknown user, in which case a user should be redirected to the
     * requested IdP. The second is that of a known user (evaluated via a
     * domain-specific refresh token read from the session), in which case
     * the user is immediately issued a token.
     *
     * @param uriInfo  The original request, in case additional data is needed.
     * @param auth     The authenticator to use to process this request.
     * @param redirect The redirect (already validated) to which the response
     *                 should be returned.
     * @param scopes   The (validated) list of scopes requested by the user.
     * @param state    The client's requested state ID.
     * @return The response, indicating success or failure.
     */
    @Override
    public Response handle(final UriInfo uriInfo,
                           final Authenticator auth,
                           final URI redirect,
                           final SortedMap<String, ApplicationScope> scopes,
                           final String state) {

        // Retrieve the authenticator instance.
        IAuthenticator authImpl = injector.getInstance(
                IAuthenticator.class, auth.getType().name());

        // Create the intermediate authorization store.
        AuthenticatorState callbackState = new AuthenticatorState();
        callbackState.setClientState(state);
        callbackState.setClientScopes(scopes);
        callbackState.setClientRedirect(redirect);
        callbackState.setAuthenticator(auth);

        // Save the state.
        session.save(callbackState);

        // Generate the redirection url.
        URI callback = uriInfo.getAbsolutePathBuilder()
                .path("/callback")
                .queryParam("state",
                        IdUtil.toString(callbackState.getId()))
                .build();

        // Run the authenticator.
        return authImpl.delegate(auth, callback);
    }

    /**
     * Handle a callback response from the IdP (Authenticator). Provided with
     * the previously stored state, this method should return to the client
     * either a valid token, or an appropriate error response.
     *
     * @param s       The request state previously saved by the client.
     * @param uriInfo The URI response from the third party IdP.
     * @return A response entity indicating success or failure.
     */
    @Override
    public Response callback(final AuthenticatorState s,
                             final UriInfo uriInfo) {
        IAuthenticator a = getAuthenticator(s);
        UserIdentity i = a.authenticate(s.getAuthenticator(),
                uriInfo.getPathParameters());

        // Build the token.
        OAuthToken t = new OAuthToken();
        t.setClient(s.getAuthenticator().getClient());
        t.setIdentity(i);
        t.setScopes(ValidationUtil
                .validateScope(s.getClientScopes(), i.getUser().getRole()));
        t.setTokenType(OAuthTokenType.Bearer);
        t.setExpiresIn(s.getAuthenticator().getClient()
                .getAccessTokenExpireIn());

        // Persist and get an ID.
        session.save(t);
        session.delete(s);

        // Build our redirect URL.
        UriBuilder responseBuilder = UriBuilder.fromUri(s.getClientRedirect());

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("access_token",
                IdUtil.toString(t.getId())));
        params.add(new BasicNameValuePair("token_type",
                t.getTokenType().toString()));
        params.add(new BasicNameValuePair("expires_in",
                String.valueOf(t.getExpiresIn())));
        if (!StringUtils.isEmpty(s.getClientState())) {
            params.add(new BasicNameValuePair("state",
                    s.getClientState()));
        }
        if (t.getScopes().size() > 0) {
            String scopeString = t.getScopes().values()
                    .stream().map(ApplicationScope::getName)
                    .collect(Collectors.joining(" "));
            params.add(new BasicNameValuePair("scope", scopeString));
        }
        responseBuilder.fragment(URLEncodedUtils.format(params, "UTF-8"));

        return Response.status(Status.FOUND)
                .location(responseBuilder.build())
                .build();
    }

    /**
     * HK2 Binder for our injector context.
     */
    @SuppressWarnings({"CPD-END"})
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(ImplicitHandler.class)
                    .to(IAuthorizeHandler.class)
                    .named(ClientType.Implicit.name())
                    .in(RequestScoped.class);
        }
    }
}
