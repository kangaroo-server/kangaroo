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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * This token type handler takes care of the "authorization_code" grant_type
 * OAuth flow. Given an authorization code provided via the auth code flow,
 * this handler will validate the request and issue a token.
 *
 * @author Michael Krotscheck
 */
public final class AuthorizationCodeGrantHandler
        implements ITokenRequestHandler {

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
    public AuthorizationCodeGrantHandler(final Session session) {
        this.session = session;
    }

    /**
     * Apply the client credentials flow to this request.
     *
     * @param client   The Client to use.
     * @param formData Raw form data for the request.
     * @return A response indicating the result of the request.
     */
    @Override
    public TokenResponseEntity handle(final Client client,
                                      final MultivaluedMap<String, String>
                                              formData) {
        // Make sure the client is the correct type.
        if (!client.getType().equals(ClientType.AuthorizationGrant)) {
            throw new InvalidGrantException();
        }

        UUID authCodeId;
        URI redirect;
        try {
            authCodeId = UUID.fromString(getOne(formData, "code"));
            redirect = UriBuilder.fromUri(getOne(formData, "redirect_uri"))
                    .build();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidGrantException();
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

        // Ensure that we retrieve a state, if it exists.
        String state = formData.getFirst("state");

        // Go ahead and create the token.
        OAuthToken newAuthToken = new OAuthToken();
        newAuthToken.setClient(client);
        newAuthToken.setTokenType(OAuthTokenType.Bearer);
        newAuthToken.setExpiresIn(client.getAccessTokenExpireIn());
        newAuthToken.setScopes(authCode.getScopes());
        newAuthToken.setIdentity(authCode.getIdentity());

        OAuthToken newRefreshToken = new OAuthToken();
        newRefreshToken.setClient(client);
        newRefreshToken.setTokenType(OAuthTokenType.Refresh);
        newRefreshToken.setExpiresIn(client.getRefreshTokenExpireIn());
        newRefreshToken.setScopes(authCode.getScopes());
        newRefreshToken.setAuthToken(newAuthToken);
        newRefreshToken.setIdentity(authCode.getIdentity());

        session.save(newAuthToken);
        session.save(newRefreshToken);
        session.delete(authCode);

        return TokenResponseEntity.factory(newAuthToken, newRefreshToken,
                state);
    }

    /**
     * Helper method, extracts one (and only one) value from a multivaluedmap.
     *
     * @param values The map of values.
     * @param key    The key to get.
     * @return The value retrieved, but only if only one exists for this key.
     */
    private String getOne(final MultivaluedMap<String, String> values,
                          final String key) {
        List<String> listValues = values.get(key);
        if (listValues == null) {
            throw new InvalidRequestException();
        }
        if (listValues.size() != 1) {
            throw new InvalidRequestException();
        }
        return listValues.get(0);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(AuthorizationCodeGrantHandler.class)
                    .to(ITokenRequestHandler.class)
                    .named("authorization_code")
                    .in(RequestScoped.class);
        }
    }
}
