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

package net.krotscheck.features.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.features.database.deserializer.AbstractEntityReferenceDeserializer;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * This entity represents an OAuth Token, provided to a user, for a specific
 * application, on a particular client's domain.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "oauth_tokens")
public final class OAuthToken extends AbstractEntity {

    /**
     * The authenticated user identity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identity", nullable = false, updatable = false)
    @JsonIgnore
    private UserIdentity identity;

    /**
     * Client for which this is valid.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client", nullable = false, updatable = false)
    @JsonIgnore
    private Client client;

    /**
     * The token type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tokenType", nullable = false)
    private OAuthTokenType tokenType = OAuthTokenType.Bearer;

    /**
     * The access token.
     */
    @Basic(optional = false)
    @Column(name = "accessToken", unique = false, nullable = false,
            updatable = false)
    private String accessToken;

    /**
     * Expires in how many seconds?.
     */
    @Basic(optional = false)
    @Column(name = "expiresIn", nullable = false)
    private long expiresIn = 3600;

    /**
     * Get the user identity to which this token was issued.
     *
     * @return The user identity.
     */
    public UserIdentity getIdentity() {
        return identity;
    }

    /**
     * Set the user identity to whom this token was issued.
     *
     * @param identity The identity.
     */
    public void setIdentity(final UserIdentity identity) {
        this.identity = identity;
    }

    /**
     * Retrieve the client for whom this token is valid.
     *
     * @return The client.
     */
    public Client getClient() {
        return client;
    }

    /**
     * Set the client for whom this token is valid.
     *
     * @param client The client.
     */
    public void setClient(final Client client) {
        this.client = client;
    }

    /**
     * Get the token type.
     *
     * @return Bearer or Authorization, as an enum.
     */
    public OAuthTokenType getTokenType() {
        return tokenType;
    }

    /**
     * Set the token type.
     *
     * @param tokenType The token type.
     */
    public void setTokenType(final OAuthTokenType tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Get the access token.
     *
     * @return The access token itself.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Set the access token.
     *
     * @param accessToken The access token itself.
     */
    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Get the token lifetime, in seconds.
     *
     * @return The lifetime of the token, in seconds.
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Set the expiration time, in seconds, from the creation date.
     *
     * @param expiresIn The time, in seconds.
     */
    public void setExpiresIn(final long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Deserialize a reference to an OAuthToken.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<OAuthToken> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(OAuthToken.class);
        }
    }
}
