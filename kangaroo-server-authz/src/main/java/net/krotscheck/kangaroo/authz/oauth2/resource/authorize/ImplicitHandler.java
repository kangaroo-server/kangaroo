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
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.HttpSession;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.common.util.ValidationUtil;
import net.krotscheck.kangaroo.authz.oauth2.authn.factory.CredentialsFactory.Credentials;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.math.BigInteger;
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
     * Request credentials.
     */
    private final Credentials credentials;

    /**
     * Current request URI.
     */
    private final UriInfo uriInfo;

    /**
     * Create a new handler.
     *
     * @param injector    The system injection manager.
     * @param session     The hibernate session.
     * @param credentials The request credentials.
     * @param uriInfo     The URI info for the current request.
     */
    @Inject
    @SuppressWarnings({"CPD-START"})
    public ImplicitHandler(final InjectionManager injector,
                           final Session session,
                           final Credentials credentials,
                           @Context final UriInfo uriInfo) {
        this.injector = injector;
        this.session = session;
        this.credentials = credentials;
        this.uriInfo = uriInfo;
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
     * @param browserSession The browser session, maintained via cookies.
     * @param auth           The authenticator to use to process this
     *                       request.
     * @param redirect       The redirect (already validated) to which
     *                       the response  should be returned.
     * @param scopes         The (validated) list of scopes requested by
     *                       the user.
     * @param state          The client's requested state ID.
     * @return The response, indicating success or failure.
     */
    @Override
    public Response handle(final javax.servlet.http.HttpSession browserSession,
                           final Authenticator auth,
                           final URI redirect,
                           final SortedMap<String, ApplicationScope> scopes,
                           final String state) {

        // Pull any refresh tokens that may already exist for this client.
        List<OAuthToken> refreshTokens = getContextToken(browserSession);

        // If there's only one refresh token, let's try to issue a refresh.
        if (refreshTokens.size() == 1) {
            OAuthToken refreshToken = refreshTokens.get(0);
            return handleRefresh(refreshToken, browserSession, redirect,
                    scopes, state);
        }

        // If we have too many refresh tokens, something weird is going on.
        // Err on the side of caution, delete them all, and fire a brand new
        // flow.
        if (refreshTokens.size() > 1) {
            refreshTokens.forEach(session::delete);
        }

        // If there's zero refresh tokens, issue a new one.
        return handleIssue(auth, redirect, scopes, state);
    }

    /**
     * This private handler presumes that we are trying to issue a brand new
     * token, and responds accordingly.
     *
     * @param auth     The authenticator to use to process this
     *                 request.
     * @param redirect The redirect (already validated) to which
     *                 the response  should be returned.
     * @param scopes   The (validated) list of scopes requested by
     *                 the user.
     * @param state    The client's requested state ID.
     * @return A response indicating the success.
     */
    private Response handleIssue(final Authenticator auth,
                                 final URI redirect,
                                 final SortedMap<String, ApplicationScope>
                                         scopes,
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
     * This private handler will issue a brand new token, based on an
     * existing refresh token.
     *
     * @param oldRefreshToken The refresh token.
     * @param browserSession  The browser session id.
     * @param redirect        The redirect (already validated) to which
     *                        the response  should be returned.
     * @param requestedScopes The (validated) list of scopes requested by
     *                        the user.
     * @param state           The client's requested state ID.
     * @return The appropriate response
     */
    private Response handleRefresh(final OAuthToken oldRefreshToken,
                                   final javax.servlet.http.HttpSession
                                           browserSession,
                                   final URI redirect,
                                   final SortedMap<String, ApplicationScope>
                                           requestedScopes,
                                   final String state) {
        String simulatedScopeRequest =
                String.join(" ", requestedScopes.keySet());

        // Make sure the requested scopes are valid for the refresh token.
        SortedMap<String, ApplicationScope> issuableScopes =
                ValidationUtil.revalidateScope(simulatedScopeRequest,
                        oldRefreshToken.getScopes(),
                        oldRefreshToken.getIdentity().getUser().getRole());

        // Go ahead and create the tokens.
        OAuthToken newBearerToken = buildBearerToken(
                oldRefreshToken.getClient(),
                oldRefreshToken.getIdentity(),
                issuableScopes, redirect);
        OAuthToken newRefreshToken = buildRefreshToken(newBearerToken);
        HttpSession dbSession = getDbSession(browserSession);

        // Persist all of our changes
        dbSession.getRefreshTokens().remove(oldRefreshToken);
        oldRefreshToken.setHttpSession(null);
        dbSession.getRefreshTokens().add(newRefreshToken);
        newRefreshToken.setHttpSession(dbSession);

        session.save(newBearerToken);
        session.save(newRefreshToken);
        if (oldRefreshToken.getAuthToken() != null) {
            session.delete(oldRefreshToken.getAuthToken());
        }
        session.delete(oldRefreshToken);
        session.getTransaction().commit();

        return buildRedirectResponse(redirect, state, newBearerToken);
    }

    /**
     * Handle a callback response from the IdP (Authenticator). Provided with
     * the previously stored state, this method should return to the client
     * either a valid token, or an appropriate error response.
     *
     * @param s              The request state previously saved by the client.
     * @param browserSession The browser session, maintained via cookies.
     * @return A response entity indicating success or failure.
     */
    @Override
    public Response callback(final AuthenticatorState s,
                             final javax.servlet.http.HttpSession
                                     browserSession) {

        URI callback = buildCallback(uriInfo, s);

        IAuthenticator a = getAuthenticator(s);
        UserIdentity identity = a.authenticate(s.getAuthenticator(),
                uriInfo.getPathParameters(), callback);
        Client client = s.getAuthenticator().getClient();
        SortedMap<String, ApplicationScope> issuedScopes = ValidationUtil
                .validateScope(s.getClientScopes(),
                        identity.getUser().getRole());

        // Build the token.
        OAuthToken accessToken = buildBearerToken(
                client, identity, issuedScopes, s.getClientRedirect());
        OAuthToken refreshToken = buildRefreshToken(accessToken);
        HttpSession dbSession = getDbSession(browserSession);

        // Persist all of our changes
        refreshToken.setHttpSession(dbSession);

        session.delete(s);
        session.save(accessToken);
        session.save(refreshToken);
        session.getTransaction().commit();

        return buildRedirectResponse(
                s.getClientRedirect(),
                s.getClientState(),
                accessToken);
    }

    /**
     * Provided with input parameters, build the redirect response for a
     * token back to the client.
     *
     * @param clientRedirect The client redirect.
     * @param clientState    The client state.
     * @param accessToken    An access token.
     * @return The response with all the necessary parameters.
     */
    private Response buildRedirectResponse(final URI clientRedirect,
                                           final String clientState,
                                           final OAuthToken accessToken) {

        // Build our redirect URL.
        UriBuilder responseBuilder = UriBuilder.fromUri(clientRedirect);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("access_token",
                IdUtil.toString(accessToken.getId())));
        params.add(new BasicNameValuePair("token_type",
                accessToken.getTokenType().toString()));
        params.add(new BasicNameValuePair("expires_in",
                String.valueOf(accessToken.getExpiresIn())));
        if (!StringUtils.isEmpty(clientState)) {
            params.add(new BasicNameValuePair("state", clientState));
        }
        if (accessToken.getScopes().size() > 0) {
            String scopeString = accessToken.getScopes().values()
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
     * Build the bearer token.
     *
     * @param client       The client.
     * @param identity     The user identity.
     * @param issuedScopes The issued scopes.
     * @param redirect     The client redirect.
     * @return A constructed, but not persisted, oauth bearer token.
     */
    private OAuthToken buildBearerToken(
            final Client client,
            final UserIdentity identity,
            final SortedMap<String, ApplicationScope> issuedScopes,
            final URI redirect) {

        // Go ahead and create the tokens.
        OAuthToken bearerToken = new OAuthToken();
        bearerToken.setClient(client);
        bearerToken.setTokenType(OAuthTokenType.Bearer);
        bearerToken.setExpiresIn(client.getAccessTokenExpireIn());
        bearerToken.setScopes(issuedScopes);
        bearerToken.setIdentity(identity);
        bearerToken.setRedirect(redirect);

        return bearerToken;
    }

    /**
     * Given a bearer token, build a refresh token.
     *
     * @param bearerToken The bearer token to 'refresh'
     * @return A constructed, but not persisted, oauth refresh token.
     */
    private OAuthToken buildRefreshToken(final OAuthToken bearerToken) {
        Client client = bearerToken.getClient();

        OAuthToken newRefreshToken = new OAuthToken();
        newRefreshToken.setClient(client);
        newRefreshToken.setTokenType(OAuthTokenType.Refresh);
        newRefreshToken.setExpiresIn(client.getRefreshTokenExpireIn());
        newRefreshToken.setScopes(bearerToken.getScopes());
        newRefreshToken.setIdentity(bearerToken.getIdentity());
        newRefreshToken.setAuthToken(bearerToken);
        newRefreshToken.setRedirect(bearerToken.getRedirect());

        return newRefreshToken;
    }

    /**
     * Provided a browser session, return the database entity that matches it.
     *
     * @param browserSession The HTTP browser session (from the servlet
     *                       container)
     * @return The DB entity.
     */
    private HttpSession getDbSession(
            final javax.servlet.http.HttpSession browserSession) {

        // Get the HTTP session
        BigInteger sessionId = IdUtil.fromString(browserSession.getId());
        return session.get(HttpSession.class, sessionId);
    }

    /**
     * Retrieve any refresh tokens associated to the current client and
     * request context.
     *
     * @param browserSession The browser session.
     * @return A list of tokens, which may be empty.
     */
    private List<OAuthToken> getContextToken(
            final javax.servlet.http.HttpSession browserSession) {

        // Get the DB session entity.
        HttpSession httpSession = getDbSession(browserSession);

        // Get the client ID.
        BigInteger clientId = credentials.getLogin();
        Client c = session.get(Client.class, clientId);

        // We have a session and we have a client...
        return httpSession.getRefreshTokens().stream()
                .filter(t -> t.getClient().equals(c))
                .filter(t -> t.getTokenType().equals(OAuthTokenType.Refresh))
                .filter(t -> !t.isExpired())
                .collect(Collectors.toList());
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
