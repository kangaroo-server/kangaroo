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

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.annotation.OAuthFilterChain;
import net.krotscheck.kangaroo.authz.oauth2.authn.factory.CredentialsFactory.Credentials;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.AuthorizationCodeGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.ClientCredentialsGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.OwnerCredentialsGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.RefreshTokenGrantHandler;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.hibernate.Session;
import org.jvnet.hk2.annotations.Optional;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.math.BigInteger;
import java.net.URI;


/**
 * The token service for our OAuth2 provider.
 *
 * @author Michael Krotscheck
 */
@Path("/token")
@PermitAll
@OAuthFilterChain
@Transactional
public final class TokenService {

    /**
     * Hibernate session to use.
     */
    private final Session session;

    /**
     * Client credentials.
     */
    private final Credentials credentials;

    /**
     * injection manager.
     */
    private final InjectionManager injector;

    /**
     * Create a new token service.
     *
     * @param session     Injected hibernate session.
     * @param credentials Injected, resolved client credentials.
     * @param injector    injection manager, to find the appropriate request
     *                    handler.
     */
    @Inject
    public TokenService(final Session session,
                        final Credentials credentials,
                        final InjectionManager injector) {
        this.session = session;
        this.credentials = credentials;
        this.injector = injector;
    }

    /**
     * Attempt to issue a token based on the requested token type.
     *
     * @param uriInfo      The raw HTTP request.
     * @param code         Authorization code (Auth code flow)
     * @param redirect     Redirect URL (Various flows)
     * @param state        Client state (all)
     * @param scope        Requested scopes (all)
     * @param username     Username (Owner Credentials)
     * @param password     Password (Owner Credentials)
     * @param refreshToken The refresh token (Refresh token flow)
     * @param grantType    The requested granttype.
     * @return The processed response.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenRequest(
            @Context final UriInfo uriInfo,
            @Optional @FormParam("code") final BigInteger code,
            @Optional @FormParam("redirect_uri") final URI redirect,
            @Optional @FormParam("state") final String state,
            @Optional @FormParam("scope") final String scope,
            @Optional @FormParam("username") final String username,
            @Optional @FormParam("password") final String password,
            @Optional @FormParam("refresh_token") final BigInteger refreshToken,
            @FormParam("grant_type") final GrantType grantType) {

        // Resolve the client - validation is handled by the filters.
        Client client = session.get(Client.class, credentials.getLogin());
        TokenResponseEntity tokenResponse = null;

        // Using if clauses here for proper coverage.
        if (GrantType.AuthorizationCode.equals(grantType)) {
            AuthorizationCodeGrantHandler handler = injector
                    .getInstance(AuthorizationCodeGrantHandler.class);
            tokenResponse = handler.handle(client, code, redirect, state);
        } else if (GrantType.ClientCredentials.equals(grantType)) {
            ClientCredentialsGrantHandler clientHandler = injector
                    .getInstance(ClientCredentialsGrantHandler.class);
            tokenResponse = clientHandler.handle(client, scope, state);
        } else if (GrantType.Password.equals(grantType)) {
            OwnerCredentialsGrantHandler passwordHandler = injector
                    .getInstance(OwnerCredentialsGrantHandler.class);
            tokenResponse = passwordHandler.handle(client, scope, state,
                    username, password);
        } else if (GrantType.RefreshToken.equals(grantType)) {
            RefreshTokenGrantHandler refreshHandler = injector
                    .getInstance(RefreshTokenGrantHandler.class);
            tokenResponse = refreshHandler.handle(client, scope, state,
                    refreshToken);
        } else {
            throw new InvalidGrantException();
        }

        // Return the token.
        return Response.ok().entity(tokenResponse).build();
    }
}
