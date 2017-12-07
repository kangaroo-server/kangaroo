/*
 * Copyright (c) 2016 Michael Krotscheck
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
 */

package net.krotscheck.kangaroo.authz.oauth2.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.AuthenticatorState;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.util.ValidationUtil;
import net.krotscheck.kangaroo.authz.oauth2.authn.annotation.OAuthFilterChain;
import net.krotscheck.kangaroo.authz.oauth2.authn.factory.CredentialsFactory.Credentials;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidRequestException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RedirectingException;
import net.krotscheck.kangaroo.authz.oauth2.resource.authorize.IAuthorizeHandler;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.hibernate.Session;
import org.jvnet.hk2.annotations.Optional;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.math.BigInteger;
import java.net.URI;
import java.util.SortedMap;


/**
 * The authorization service for our OAuth2 provider.
 *
 * @author Michael Krotscheck
 */
@Path("/authorize")
@PermitAll
@Transactional
@Api(tags = "OAuth2")
public final class AuthorizationService {

    /**
     * Hibernate session to use.
     */
    private final Session session;

    /**
     * Client credentials.
     */
    private final Credentials credentials;

    /**
     * The injection manager.
     */
    private final InjectionManager injector;

    /**
     * Create a new authorization service.
     *
     * @param session     Injected hibernate session.
     * @param credentials Injected, resolved client credentials.
     * @param injector    Injected injection manager
     */
    @Inject
    public AuthorizationService(final Session session,
                                final Credentials credentials,
                                final InjectionManager injector) {
        this.session = session;
        this.credentials = credentials;
        this.injector = injector;
    }

    /**
     * Attempt to execute an authorization request based on the provided
     * request parameters.
     *
     * @param uriInfo       The requested URI.
     * @param request       The servlet request.
     * @param authenticator A string key for the authenticator that should be
     *                      used.
     * @param responseType  The requested response_type.
     * @param state         An XSS Busting state string.
     * @param scope         A requested authorization scope.
     * @param redirectUrl   The requested redirect uri.
     * @return The processed response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @OAuthFilterChain
    @ApiOperation(value = "OAuth2 Authorization endpoint.")
    public Response authorizationRequest(
            @Context final UriInfo uriInfo,
            @Context final HttpServletRequest request,
            @Optional @QueryParam("authenticator")
            final AuthenticatorType authenticator,
            @ApiParam(required = true, allowableValues = "code,token")
            @Optional @QueryParam("response_type") final String responseType,
            @Optional @QueryParam("redirect_uri") final String redirectUrl,
            @ApiParam(example = "scope1 scope2")
            @Optional @QueryParam("scope") final String scope,
            @Optional @QueryParam("state") final String state) {

        // With a valid request, we need to make sure a browser session is
        // established. Even though we only use it for the implicit flow, the
        // hit is valid for keeping the session alive.
        HttpSession httpSession = request.getSession(true);

        // Make sure the kind of request we have is permitted for this client.
        Client client = session.get(Client.class, credentials.getLogin());

        // Validate the redirect.
        URI redirect = ValidationUtil.requireValidRedirect(redirectUrl,
                client.getRedirects());

        try {
            // Validate the response type.
            ValidationUtil.validateResponseType(client, responseType);

            // Validate the authenticator
            Authenticator auth = ValidationUtil.validateAuthenticator(
                    authenticator, client.getAuthenticators());

            // Retrieve the client.
            Client c = auth.getClient();

            // Validate the requested scopes.
            SortedMap<String, ApplicationScope> scopes =
                    ValidationUtil.validateScope(scope,
                            client.getApplication().getScopes());

            IAuthorizeHandler handler =
                    injector.getInstance(IAuthorizeHandler.class,
                            c.getType().toString());

            Response response = handler.handle(httpSession, auth,
                    redirect, scopes, state);

            // On success, rotate the session id.
            request.changeSessionId();

            return response;
        } catch (KangarooException e) {
            // Any caught exceptions here should be redirected to the
            // validated redirect_url instead.
            throw new RedirectingException(e, redirect, client.getType());
        }
    }

    /**
     * Handle a callback from an authentication provider.
     *
     * @param state   State passed to the authenticator to include in the
     *                callback.
     * @param request The servlet request.
     * @param uriInfo The URI, including all result parameters.
     * @return A response, describing the request.
     */
    @GET
    @Path("/callback")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "3rd Party IdP Callback", hidden = true)
    public Response authorizationCallback(
            @Context final UriInfo uriInfo,
            @Context final HttpServletRequest request,
            @Optional
            @DefaultValue("")
            @QueryParam("state") final String state) {

        // With a valid request, we need to make sure a browser session is
        // established. Even though we only use it for the implicit flow, the
        // hit is valid for keeping the session alive.
        HttpSession httpSession = request.getSession(true);

        // These next two lines are null-safe.
        AuthenticatorState s = getAuthenticatorState(state);
        Client c = s.getAuthenticator().getClient();

        try {
            IAuthorizeHandler handler =
                    injector.getInstance(IAuthorizeHandler.class,
                            c.getType().toString());

            // Just in case this was linked to an invalid client type...
            if (handler == null) {
                throw new InvalidRequestException();
            }

            Response response = handler.callback(s, httpSession);

            // On success, rotate the session id.
            request.changeSessionId();

            return response;
        } catch (KangarooException e) {
            // Any caught exceptions here should be redirected to the
            // validated redirect_url instead.
            throw new RedirectingException(e, s.getClientRedirect(),
                    c.getType());
        }
    }

    /**
     * Attempt to resolve an intermediate, stored authenticator state from a
     * provided string.
     *
     * @param stateString The string.
     * @return A state.
     */
    private AuthenticatorState getAuthenticatorState(final String stateString) {
        try {
            BigInteger id = IdUtil.fromString(stateString);
            AuthenticatorState callbackState =
                    session.get(AuthenticatorState.class, id);
            if (callbackState == null) {
                throw new InvalidRequestException();
            }
            return callbackState;
        } catch (Exception e) {
            throw new InvalidRequestException();
        }
    }
}
