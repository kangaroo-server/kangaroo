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
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.SortedMap;

/**
 * This handler is passed all authorization code requests.
 *
 * @author Michael Krotscheck
 */
public final class AuthCodeHandler implements IAuthorizeHandler {

    /**
     * The injection manager.
     */
    private final InjectionManager injector;

    /**
     * The active database session.
     */
    private final Session session;

    /**
     * Create a new handler.
     *
     * @param injector The injection manager.
     * @param session  The hibernate session.
     */
    @Inject
    @SuppressWarnings({"CPD-START"})
    public AuthCodeHandler(final InjectionManager injector,
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
     * Handle an authorization request using the Auth Code flow.
     *
     * @param uriInfo        The original request, in case additional data
     *                       is needed.
     * @param browserSession The browser session, maintained via cookies.
     * @param auth           The authenticator to use to process this
     *                       request.
     * @param redirect       The redirect (already validated) to which
     *                       the response  should be returned.
     * @param scopes         The (validated) list of scopes requested by
     *                       the user.
     * @param state          The client's requested state ID.
     * @return The response from the handler.
     */
    @Override
    public Response handle(final UriInfo uriInfo,
                           final HttpSession browserSession,
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
        URI callback = buildCallback(uriInfo, callbackState);

        // Run the authenticator.
        return authImpl.delegate(auth, callback);
    }

    /**
     * Handle a callback response from the IdP (Authenticator). Provided with
     * the previously stored state, this method should return to the client
     * either a valid token, or an appropriate error response.
     *
     * @param s              The request state previously saved by the client.
     * @param browserSession The browser session, maintained via cookies.
     * @param uriInfo        The URI response from the third party IdP.
     * @return A response entity indicating success or failure.
     */
    @Override
    public Response callback(final AuthenticatorState s,
                             final HttpSession browserSession,
                             final UriInfo uriInfo) {

        URI callback = buildCallback(uriInfo, s);

        IAuthenticator a = getAuthenticator(s);
        UserIdentity i = a.authenticate(s.getAuthenticator(),
                uriInfo.getPathParameters(), callback);

        // Build the token.
        OAuthToken t = new OAuthToken();
        t.setClient(s.getAuthenticator().getClient());
        t.setIdentity(i);
        t.setScopes(ValidationUtil
                .validateScope(s.getClientScopes(), i.getUser().getRole()));
        t.setTokenType(OAuthTokenType.Authorization);
        t.setExpiresIn(s.getAuthenticator().getClient()
                .getAuthorizationCodeExpiresIn());
        t.setRedirect(s.getClientRedirect());
        t.setIdentity(i);

        // Persist and get an ID.
        session.save(t);
        session.delete(s);

        // Build our redirect URL.
        UriBuilder responseBuilder = UriBuilder.fromUri(s.getClientRedirect());
        responseBuilder.queryParam("code", IdUtil.toString(t.getId()));
        if (!StringUtils.isEmpty(s.getClientState())) {
            responseBuilder.queryParam("state", s.getClientState());
        }

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
            bind(AuthCodeHandler.class)
                    .to(IAuthorizeHandler.class)
                    .named(ClientType.AuthorizationGrant.name())
                    .in(RequestScoped.class);
        }
    }
}
