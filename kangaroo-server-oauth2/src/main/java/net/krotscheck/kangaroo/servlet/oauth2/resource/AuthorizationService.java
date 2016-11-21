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

package net.krotscheck.kangaroo.servlet.oauth2.resource;

import net.krotscheck.kangaroo.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidRequestException;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.AuthenticatorState;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.servlet.oauth2.annotation.OAuthFilterChain;
import net.krotscheck.kangaroo.servlet.oauth2.factory.CredentialsFactory.Credentials;
import net.krotscheck.kangaroo.util.ValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.glassfish.hk2.api.ServiceLocator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jvnet.hk2.annotations.Optional;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;


/**
 * The authorization service for our OAuth2 provider.
 *
 * @author Michael Krotscheck
 */
@Path("/authorize")
@PermitAll
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
     * The Service Locator.
     */
    private final ServiceLocator locator;

    /**
     * Create a new authorization service.
     *
     * @param session     Injected hibernate session.
     * @param credentials Injected, resolved client credentials.
     * @param locator     Injected service locator
     */
    @Inject
    public AuthorizationService(final Session session,
                                final Credentials credentials,
                                final ServiceLocator locator) {
        this.session = session;
        this.credentials = credentials;
        this.locator = locator;
    }

    /**
     * Attempt to execute an authorization request based on the provided
     * request parameters.
     *
     * @param uriInfo       The requested URI.
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
    public Response authorizationRequest(
            @Context
            final UriInfo uriInfo,
            @Optional @QueryParam("authenticator")
            final String authenticator,
            @Optional @QueryParam("response_type")
            final String responseType,
            @Optional @QueryParam("redirect_uri")
            final String redirectUrl,
            @Optional @QueryParam("scope")
            final String scope,
            @Optional @QueryParam("state")
            final String state) {

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

            // Ensure that we have a valid authenticator type.
            IAuthenticator authImpl = locator.getService(
                    IAuthenticator.class, auth.getType());
            if (authImpl == null) {
                throw new InvalidRequestException();
            }

            // Validate the requested scopes.
            SortedMap<String, ApplicationScope> scopes =
                    ValidationUtil.validateScope(scope,
                            client.getApplication().getScopes());

            // Create the intermediate authorization store.
            AuthenticatorState callbackState = new AuthenticatorState();
            callbackState.setClient(client);
            callbackState.setClientState(state);
            callbackState.setClientScopes(scopes);
            callbackState.setClientRedirect(redirect);
            callbackState.setAuthenticator(auth);

            // Save the state.
            Transaction t = session.beginTransaction();
            session.save(callbackState);
            t.commit();

            // Generate the redirection url.
            URI callback = uriInfo.getAbsolutePathBuilder()
                    .path("/callback")
                    .queryParam("state", callbackState.getId())
                    .build();

            // Run the authenticator.
            return authImpl.delegate(auth, callback);
        } catch (HttpStatusException e) {
            // Any caught exceptions here should be redirected to the
            // validated redirect_url instead.
            ErrorResponseBuilder builder = ErrorResponseBuilder
                    .from(e.getHttpStatus(),
                            e.getMessage(),
                            e.getErrorCode(),
                            redirect);
            return builder.build(ClientType.Implicit.equals(client.getType()));
        }
    }

    /**
     * Handle a callback from an authentication provider.
     *
     * @param state   State passed to the authenticator to include in the
     *                callback.
     * @param uriInfo The URI, including all result parameters.
     * @return A response, describing the request.
     */
    @GET
    @Path("/callback")
    @Produces(MediaType.APPLICATION_JSON)
    public Response authorizationCallback(
            @Context
            final UriInfo uriInfo,
            @Optional @DefaultValue("") @QueryParam("state")
            final String state) {
        // Resolve various necessary components.
        AuthenticatorState s = getAuthenticatorState(state);

        try {
            IAuthenticator a = getAuthenticator(s);
            UserIdentity i = a.authenticate(s.getAuthenticator(),
                    uriInfo.getPathParameters());

            // Since this code won't execute without a valid state, we should be
            // safe to do a simple if/else here. Except we're paranoid.
            if (s.getClient().getType() == ClientType.AuthorizationGrant) {
                return handleGrantResponse(s, i);
            } else if (s.getClient().getType() == ClientType.Implicit) {
                return handleImplicitResponse(s, i);
            } else {
                throw new InvalidRequestException();
            }
        } catch (HttpStatusException e) {
            // Any caught exceptions here should be redirected to the
            // validated redirect_url instead.
            ErrorResponseBuilder builder = ErrorResponseBuilder
                    .from(e.getHttpStatus(),
                            e.getMessage(),
                            e.getErrorCode(),
                            s.getClientRedirect());
            return builder.build(ClientType.Implicit
                    .equals(s.getClient().getType()));
        }
    }

    /**
     * Construct an OAuth Token response for an implicit client.
     *
     * @param s The intermediate authenticator state.
     * @param i The resolved user identity.
     * @return An HTTP response with the granted token response.
     */
    private Response handleImplicitResponse(final AuthenticatorState s,
                                            final UserIdentity i) {
        // Build the token.
        OAuthToken t = new OAuthToken();
        t.setClient(s.getClient());
        t.setIdentity(i);
        t.setScopes(ValidationUtil
                .validateScope(s.getClientScopes(), i.getUser().getRole()));
        t.setTokenType(OAuthTokenType.Bearer);
        t.setExpiresIn(s.getClient().getAccessTokenExpireIn());

        // Persist and get an ID.
        Transaction transaction = session.beginTransaction();
        session.save(t);
        session.delete(s);
        transaction.commit();

        // Build our redirect URL.
        UriBuilder responseBuilder = UriBuilder.fromUri(s.getClientRedirect());

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("access_token",
                t.getId().toString()));
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

        return Response.status(HttpStatus.SC_MOVED_TEMPORARILY)
                .location(responseBuilder.build())
                .build();
    }

    /**
     * Construct an Authorization Code response for an authorization grant
     * client.
     *
     * @param s The intermediate authenticator state.
     * @param i The resolved user identity.
     * @return An HTTP response with the granted token response.
     */
    private Response handleGrantResponse(final AuthenticatorState s,
                                         final UserIdentity i) {
        // Build the token.
        OAuthToken t = new OAuthToken();
        t.setClient(s.getClient());
        t.setIdentity(i);
        t.setScopes(ValidationUtil
                .validateScope(s.getClientScopes(), i.getUser().getRole()));
        t.setTokenType(OAuthTokenType.Authorization);
        t.setExpiresIn(s.getClient().getAuthorizationCodeExpiresIn());
        t.setRedirect(s.getClientRedirect());

        // Persist and get an ID.
        Transaction transaction = session.beginTransaction();
        session.save(t);
        session.delete(s);
        transaction.commit();

        // Build our redirect URL.
        UriBuilder responseBuilder = UriBuilder.fromUri(s.getClientRedirect());
        responseBuilder.queryParam("code", t.getId().toString());
        if (!StringUtils.isEmpty(s.getClientState())) {
            responseBuilder.queryParam("state", s.getClientState());
        }

        return Response.status(HttpStatus.SC_MOVED_TEMPORARILY)
                .location(responseBuilder.build())
                .build();
    }

    /**
     * Provided a stored intermediate authenticator state, attempt to resolve
     * an instance of the associated authenticator implementation.
     *
     * @param state The state to resolve.
     * @return An authenticator Impl, available from the injection context.
     */
    private IAuthenticator getAuthenticator(final AuthenticatorState state) {
        Authenticator a = state.getAuthenticator();
        IAuthenticator authenticator =
                locator.getService(IAuthenticator.class, a.getType());
        if (authenticator == null) {
            throw new InvalidRequestException();
        }
        return authenticator;
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
            UUID id = UUID.fromString(stateString);
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
