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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.stream.Collectors;

/**
 * A POJO that represents the response from the introspection endpoint.
 *
 * @author Michael Krotscheck
 */
@JsonInclude(Include.NON_NULL)
public final class IntrospectionResponseEntity {

    /**
     * REQUIRED.  Boolean indicator of whether or not the presented token
     * is currently active.  The specifics of a token's "active" state
     * will vary depending on the implementation of the authorization
     * server and the information it keeps about its tokens, but a "true"
     * value return for the "active" property will generally indicate
     * that a given token has been issued by this authorization server,
     * has not been revoked by the resource owner, and is within its
     * given time window of validity (e.g., after its issuance time and
     * before its expiration time).  See Section 4 for information on
     * implementation of such checks.
     */
    private boolean active = false;

    /**
     * OPTIONAL.  A JSON string containing a space-separated list of
     * scopes associated with this token, in the format described in
     * Section 3.3 of OAuth 2.0 [RFC6749].
     */
    private String scope;

    /**
     * OPTIONAL.  Client identifier for the OAuth 2.0 client that
     * requested this token.
     */
    @JsonProperty("client_id")
    private BigInteger clientId;

    /**
     * OPTIONAL.  Human-readable identifier for the resource owner who
     * authorized this token.
     */
    private String username;

    /**
     * OPTIONAL.  Type of the token as defined in Section 5.1 of OAuth
     * 2.0 [RFC6749].
     */
    @JsonProperty("token_type")
    private OAuthTokenType tokenType;

    /**
     * OPTIONAL.  Integer timestamp, measured in the number of seconds
     * since January 1 1970 UTC, indicating when this token will expire,
     * as defined in JWT [RFC7519].
     */
    private Calendar exp;

    /**
     * OPTIONAL.  Integer timestamp, measured in the number of seconds
     * since January 1 1970 UTC, indicating when this token was
     * originally issued, as defined in JWT [RFC7519].
     */
    private Calendar iat;

    /**
     * OPTIONAL.  Integer timestamp, measured in the number of seconds
     * since January 1 1970 UTC, indicating when this token is not to be
     * used before, as defined in JWT [RFC7519].
     */
    private Calendar nbf;

    /**
     * OPTIONAL.  Subject of the token, as defined in JWT [RFC7519].
     * Usually a machine-readable identifier of the resource owner who
     * authorized this token.
     */
    private BigInteger sub;

    /**
     * OPTIONAL.  Service-specific string identifier or list of string
     * identifiers representing the intended audience for this token, as
     * defined in JWT [RFC7519].
     */
    private BigInteger aud;

    /**
     * OPTIONAL.  String representing the issuer of this token, as
     * defined in JWT [RFC7519].
     */
    private String iss;

    /**
     * OPTIONAL.  String identifier for the token, as defined in JWT
     * [RFC7519].
     */
    private BigInteger jti;

    /**
     * Create a new IntrospectionResponseEntity from a token.
     *
     * @param token The token.
     */
    public IntrospectionResponseEntity(final OAuthToken token) {
        this.active = token != null && !token.isExpired();

        // If the token is not active, return now- no additional data may be
        // sent.
        if (!this.active) {
            return;
        }

        this.tokenType = token.getTokenType();
        this.clientId = token.getClient().getId();
        this.aud = token.getClient().getApplication().getId();
        this.jti = token.getId();

        if (token.getIdentity() != null) {
            this.username = token.getIdentity().getRemoteId();
            this.sub = token.getIdentity().getUser().getId();
        } else {
            this.sub = token.getClient().getId();
        }

        Calendar future = (Calendar) token.getCreatedDate().clone();
        future.add(Calendar.SECOND, token.getExpiresIn().intValue());
        this.exp = future;

        this.iat = (Calendar) token.getCreatedDate().clone();
        this.nbf = (Calendar) token.getCreatedDate().clone();

        this.scope = token.getScopes().keySet()
                .stream()
                .collect(Collectors.joining(" "));

        this.iss = token.getIssuer();
    }

    /**
     * Create a new IntrospectionResponseEntity with default values
     * (mostly null).
     */
    public IntrospectionResponseEntity() {
    }

    /**
     * The current active state of this token.
     *
     * @return True if it's active, otherwise false.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * The list of scopes authorized for this token.
     *
     * @return A space-separated list of scopes.
     */
    public String getScope() {
        return scope;
    }

    /**
     * The client ID.
     *
     * @return The client ID.
     */
    public BigInteger getClientId() {
        return clientId;
    }

    /**
     * The username, if applicable.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Return the token type.
     *
     * @return The token type.
     */
    public OAuthTokenType getTokenType() {
        return tokenType;
    }

    /**
     * The expiration timestamp.
     *
     * @return The time when this token expires.
     */
    public Calendar getExp() {
        return exp;
    }

    /**
     * Issued At timestamp.
     *
     * @return The date this token was issued.
     */
    public Calendar getIat() {
        return iat;
    }

    /**
     * Not-Before timestamp.
     *
     * @return The time _before which_ this token may not be used.
     */
    public Calendar getNbf() {
        return nbf;
    }

    /**
     * The token subject, usually the resource owner.
     *
     * @return The ID of the resource owner.
     */
    public BigInteger getSub() {
        return sub;
    }

    /**
     * Intended audience, in our case the Client ID.
     *
     * @return The client ID.
     */
    public BigInteger getAud() {
        return aud;
    }

    /**
     * The issuer, as in the entity that issued this token.
     *
     * @return The URL of this kangaroo server.
     */
    public String getIss() {
        return iss;
    }

    /**
     * The token ID.
     *
     * @return String identifier for the token (the ID).
     */
    public BigInteger getJti() {
        return jti;
    }
}
