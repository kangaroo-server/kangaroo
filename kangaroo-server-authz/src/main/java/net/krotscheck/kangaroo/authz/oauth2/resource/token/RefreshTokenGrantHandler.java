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

import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.util.ValidationUtil;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.SortedMap;


/**
 * This token type handler takes care of the "refresh_token" grant_type
 * OAuth flow. Its job is to handle all refresh token requests, by
 * invalidating any previous tokens and issuing new ones.
 *
 * @author Michael Krotscheck
 */
public final class RefreshTokenGrantHandler {

    /**
     * Hibernate session, injected.
     */
    private final Session session;

    /**
     * Create a new instance of this token handler.
     *
     * @param session Injected hibernate session.
     */
    @Inject
    public RefreshTokenGrantHandler(final Session session) {
        this.session = session;
    }

    /**
     * Handle a specific token type request.
     *
     * @param client    The client.
     * @param scope     The requested scopes.
     * @param state     The state.
     * @param refreshId ID of the refresh token.
     * @return A token response entity with the new token.
     */
    public TokenResponseEntity handle(final Client client,
                                      final String scope,
                                      final String state,
                                      final BigInteger refreshId) {
        // Make sure the client is the correct type.
        ClientType type = client.getType();
        if (!type.equals(ClientType.OwnerCredentials)
                && !type.equals(ClientType.AuthorizationGrant)) {
            throw new InvalidGrantException();
        }

        // If we have no refresh ID, throw an error.
        if (refreshId == null) {
            throw new InvalidGrantException();
        }

        // Attempt to get the refresh token
        OAuthToken refreshToken = session.get(OAuthToken.class, refreshId);
        if (refreshToken == null || !refreshToken.getTokenType()
                .equals(OAuthTokenType.Refresh)) {
            throw new InvalidGrantException();
        }

        // Make sure the refresh token is not expired.
        if (refreshToken.isExpired()) {
            throw new InvalidGrantException();
        }

        // Make sure the requested scopes are valid for the refresh token.
        SortedMap<String, ApplicationScope> requestedScopes =
                ValidationUtil.revalidateScope(
                        scope,
                        refreshToken.getScopes(),
                        refreshToken.getIdentity().getUser().getRole()
                );

        // Go ahead and create the tokens.
        OAuthToken newAuthToken = new OAuthToken();
        newAuthToken.setClient(client);
        newAuthToken.setTokenType(OAuthTokenType.Bearer);
        newAuthToken.setExpiresIn(client.getAccessTokenExpireIn());
        newAuthToken.setScopes(requestedScopes);
        newAuthToken.setIdentity(refreshToken.getIdentity());

        OAuthToken newRefreshToken = new OAuthToken();
        newRefreshToken.setClient(client);
        newRefreshToken.setTokenType(OAuthTokenType.Refresh);
        newRefreshToken.setExpiresIn(client.getRefreshTokenExpireIn());
        newRefreshToken.setScopes(requestedScopes);
        newRefreshToken.setIdentity(refreshToken.getIdentity());
        newRefreshToken.setAuthToken(newAuthToken);

        session.save(newAuthToken);
        session.save(newRefreshToken);
        if (refreshToken.getAuthToken() != null) {
            session.delete(refreshToken.getAuthToken());
        }
        session.delete(refreshToken);

        return TokenResponseEntity.factory(newAuthToken, newRefreshToken,
                state);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(RefreshTokenGrantHandler.class)
                    .to(RefreshTokenGrantHandler.class)
                    .in(RequestScoped.class);
        }
    }
}
