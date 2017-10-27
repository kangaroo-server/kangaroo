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

package net.krotscheck.kangaroo.authz.common.authenticator.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A small, class-private oauth token pojo. Describes all properties as
 * strings, as we don't know what third party OAuth2 IdP's will send us.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2IdPToken {

    /**
     * The access token.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * The token type (bearer or authorization).
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * When the token expires, in seconds.
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * The refresh token.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

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
     * Retrieve the access token.
     *
     * @return The access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Set the access token.
     *
     * @param accessToken The new access token.
     */
    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Get the token type!
     *
     * @return The token type.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Set the token type.
     *
     * @param tokenType The token type.
     */
    public void setTokenType(final String tokenType) {
        this.tokenType = tokenType;
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
     * Set the token expiry.
     *
     * @param expiresIn The expiry.
     */
    public void setExpiresIn(final Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Get the refresh token.
     *
     * @return The refresh token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Set the refresh token.
     *
     * @param refreshToken The refresh token.
     */
    public void setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
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
     * Set the returned scopes.
     *
     * @param scope The scopes.
     */
    public void setScope(final String scope) {
        this.scope = scope;
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
     * Set any response state issued with the request.
     *
     * @param state The response state.
     */
    public void setState(final String state) {
        this.state = state;
    }
}
