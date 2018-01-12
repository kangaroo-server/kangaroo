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
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.AuthorizationCodeGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.ClientCredentialsGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.OwnerCredentialsGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.RefreshTokenGrantHandler;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.util.ObjectUtil;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.jvnet.hk2.annotations.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.math.BigInteger;
import java.net.URI;


/**
 * The token service for our OAuth2 provider.
 *
 * @author Michael Krotscheck
 */
@Path("/token")
@Transactional
@Api(tags = "OAuth2")
public final class TokenService {

    /**
     * The request security context..
     */
    private final SecurityContext securityContext;

    /**
     * Injection manager.
     */
    private final InjectionManager injector;

    /**
     * Create a new token service.
     *
     * @param securityContext Injected, resolved security context.
     * @param injector        injection manager, to find the appropriate request
     *                        handler.
     */
    @Inject
    public TokenService(final SecurityContext securityContext,
                        final InjectionManager injector) {
        this.securityContext = securityContext;
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
    @ApiOperation(value = "OAuth2 Token endpoint.")
    @ApiParam(name = "token")
    @O2Client
    public Response tokenRequest(
            @Context final UriInfo uriInfo,
            @ApiParam(type = "string")
            @Optional @FormParam("code") final BigInteger code,
            @Optional @FormParam("redirect_uri") final URI redirect,
            @Optional @FormParam("state") final String state,
            @Optional @FormParam("scope") final String scope,
            @Optional @FormParam("username") final String username,
            @Optional @FormParam("password") final String password,
            @ApiParam(type = "string")
            @Optional @FormParam("refresh_token") final BigInteger refreshToken,
            @ApiParam(required = true, allowableValues = "authorization_code,"
                    + "client_credentials,password,refresh_token")
            @FormParam("grant_type") final GrantType grantType) {

        O2Principal principal = ObjectUtil
                .safeCast(securityContext.getUserPrincipal(), O2Principal.class)
                .orElseThrow(AccessDeniedException::new);

        // Resolve the client - validation is handled by the filters.
        Client client = principal.getContext();
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
