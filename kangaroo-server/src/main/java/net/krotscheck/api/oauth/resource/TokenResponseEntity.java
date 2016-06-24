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

package net.krotscheck.api.oauth.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A POJO that represents the response from the token endpoint.
 *
 * @author Michael Krotscheck
 */
@JsonInclude(Include.NON_NULL)
public final class TokenResponseEntity {

    /**
     * The access token.
     */
    @JsonProperty("access_token")
    private UUID accessToken;

    /**
     * The token type (bearer or authorization).
     */
    @JsonProperty("token_type")
    private OAuthTokenType tokenType;

    /**
     * When the token expires, in seconds.
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * The refresh token.
     */
    @JsonProperty("refresh_token")
    private UUID refreshToken;

    /**
     * The requested scope from the original authorization request.
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * The token response may contain a state, provided by the client.
     */
    @JsonProperty("state")
    private String state;

    /**
     * Private constructor, use factory istead.
     */
    private TokenResponseEntity() {

    }

    /**
     * Retrieve the access token.
     *
     * @return The access token.
     */
    public UUID getAccessToken() {
        return accessToken;
    }

    /**
     * Get the token type!
     *
     * @return The token type.
     */
    public OAuthTokenType getTokenType() {
        return tokenType;
    }

    /**
     * Get the expiration time, in seconds.
     *
     * @return The expiration time, in seconds.
     */
    public Long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Get the refresh token.
     *
     * @return The refresh token.
     */
    public UUID getRefreshToken() {
        return refreshToken;
    }

    /**
     * Get the scope.
     *
     * @return The token scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Retrieve the state for the token response.
     *
     * @return A provided state, or null.
     */
    public String getState() {
        return state;
    }

    /**
     * Create this entity from an OAuthToken.
     *
     * @param token The token from which we're constructing this response.
     * @param state A request provided state string, passed back to the
     *              client verbatim.
     * @return A new token.
     */
    public static TokenResponseEntity factory(final OAuthToken token,
                                              final String state) {
        TokenResponseEntity t = new TokenResponseEntity();
        t.accessToken = token.getId();
        t.tokenType = token.getTokenType();
        t.expiresIn = token.getExpiresIn();
        t.state = state;

        List<String> scopes = new ArrayList<>();
        token.getScopes().forEach((n, s) -> scopes.add(n));
        if (scopes.size() > 0) {
            t.scope = String.join(" ", scopes);
        }
        return t;
    }

    /**
     * Create this entity from an OAuthToken.
     *
     * @param token   The token from which we're constructing this response.
     * @param refresh The refresh token for this response.
     * @param state   A request provided state string, passed back to the
     *                client verbatim.
     * @return A new token.
     */
    public static TokenResponseEntity factory(final OAuthToken token,
                                              final OAuthToken refresh,
                                              final String state) {
        TokenResponseEntity t = factory(token, state);
        t.refreshToken = refresh.getId();
        return t;
    }
}
