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

package net.krotscheck.kangaroo.authz.oauth2.resource.token;

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidRequestException;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.math.BigInteger;
import java.net.URI;


/**
 * This token type handler takes care of the "authorization_code" grant_type
 * OAuth flow. Given an authorization code provided via the auth code flow,
 * this handler will validate the request and issue a token.
 *
 * @author Michael Krotscheck
 */
public final class AuthorizationCodeGrantHandler {

    /**
     * Hibernate session, injected.
     */
    private final Session session;

    /**
     * Current request URI.
     */
    private final UriInfo uriInfo;

    /**
     * Create a new instance of this token handler.
     *
     * @param session Injected hibernate session.
     * @param uriInfo The URI info for the current request.
     */
    @Inject
    public AuthorizationCodeGrantHandler(final Session session,
                                         @Context final UriInfo uriInfo) {
        this.session = session;
        this.uriInfo = uriInfo;
    }

    /**
     * Apply the client credentials flow to this request.
     *
     * @param client     The Client to use.
     * @param authCodeId The authorization code.
     * @param redirect   The redirect URI.
     * @param state      The state.
     * @return A response indicating the result of the request.
     */
    public TokenResponseEntity handle(final Client client,
                                      final BigInteger authCodeId,
                                      final URI redirect,
                                      final String state) {

        // Make sure the client is the correct type.
        if (!client.getType().equals(ClientType.AuthorizationGrant)) {
            throw new InvalidGrantException();
        }

        if (authCodeId == null || redirect == null) {
            throw new InvalidRequestException();
        }

        // Retrieve the authorization code
        OAuthToken authCode = session.get(OAuthToken.class, authCodeId);
        if (authCode == null) {
            throw new InvalidGrantException();
        }

        // Check to see if it's expired.
        if (authCode.isExpired()) {
            throw new InvalidGrantException();
        }

        // Make sure that the client and auth code match
        if (!authCode.getClient().equals(client)) {
            throw new InvalidGrantException();
        }

        // Ensure that the redirect URL matches.
        if (!redirect.equals(authCode.getRedirect())) {
            throw new InvalidGrantException();
        }

        // Go ahead and create the token.
        OAuthToken newAuthToken = new OAuthToken();
        newAuthToken.setClient(client);
        newAuthToken.setTokenType(OAuthTokenType.Bearer);
        newAuthToken.setExpiresIn(client.getAccessTokenExpireIn());
        newAuthToken.setScopes(authCode.getScopes());
        newAuthToken.setIdentity(authCode.getIdentity());
        newAuthToken.setIssuer(uriInfo.getAbsolutePath().getHost());

        OAuthToken newRefreshToken = new OAuthToken();
        newRefreshToken.setClient(client);
        newRefreshToken.setTokenType(OAuthTokenType.Refresh);
        newRefreshToken.setExpiresIn(client.getRefreshTokenExpireIn());
        newRefreshToken.setScopes(authCode.getScopes());
        newRefreshToken.setAuthToken(newAuthToken);
        newRefreshToken.setIdentity(authCode.getIdentity());
        newRefreshToken.setIssuer(uriInfo.getAbsolutePath().getHost());

        session.save(newAuthToken);
        session.save(newRefreshToken);
        session.delete(authCode);

        return TokenResponseEntity.factory(newAuthToken, newRefreshToken,
                state);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(AuthorizationCodeGrantHandler.class)
                    .to(AuthorizationCodeGrantHandler.class)
                    .in(RequestScoped.class);
        }
    }
}
