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
import org.hibernate.annotations.SortNatural;

import java.net.URI;
import java.util.Calendar;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
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
    @JoinColumn(name = "identity", nullable = true, updatable = false)
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
     * The parent auth token (used for refresh tokens only).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authToken", nullable = true, updatable = false)
    @JsonIgnore
    private OAuthToken authToken;

    /**
     * The token type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tokenType", nullable = false)
    private OAuthTokenType tokenType = OAuthTokenType.Bearer;

    /**
     * Expires in how many seconds?
     */
    @Basic(optional = false)
    @Column(name = "expiresIn", nullable = false)
    private long expiresIn = 600;

    /**
     * Authorization Codes must keep track of the redirect they were issued
     * for.
     */
    @Basic
    @Column(name = "redirect", nullable = true)
    private URI redirect;

    /**
     * List of the application's scopes.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "oauth_token_scopes",
            joinColumns = {
                    @JoinColumn(name = "token",
                            nullable = false, updatable = false)},
            inverseJoinColumns = {
                    @JoinColumn(name = "scope",
                            nullable = false, updatable = false)})
    @JsonIgnore
    @MapKey(name = "name")
    @SortNatural
    private SortedMap<String, ApplicationScope> scopes;

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
     * Retrieve the oauth token for this refresh token.
     *
     * @return The auth token.
     */
    public OAuthToken getAuthToken() {
        return authToken;
    }

    /**
     * Set the new parent auth token (for refresh tokens only).
     *
     * @param authToken The new auth token.
     */
    public void setAuthToken(final OAuthToken authToken) {
        this.authToken = authToken;
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
     * Get this token's scopes.
     *
     * @return A map of scopes.
     */
    public SortedMap<String, ApplicationScope> getScopes() {
        return scopes;
    }

    /**
     * Set this token's scopes.
     *
     * @param scopes A new map of scopes.
     */
    public void setScopes(final SortedMap<String, ApplicationScope> scopes) {
        this.scopes = new TreeMap<>(scopes);
    }

    /**
     * This method returns true if the created time, plus the expiration
     * seconds, is less than the current time.
     *
     * @return True of this token is expired, otherwise false.
     */
    @JsonIgnore
    public boolean isExpired() {
        if (getCreatedDate() == null) {
            return true;
        }

        Calendar now = Calendar.getInstance();
        Calendar expireDate = (Calendar) getCreatedDate().clone();
        expireDate.add(Calendar.SECOND, (int) getExpiresIn());
        return now.after(expireDate);
    }

    /**
     * Get the redirect attached to this token.
     *
     * @return The redirect.
     */
    public URI getRedirect() {
        return redirect;
    }

    /**
     * Set the redirect for this particular token.
     *
     * @param redirect The new redirect.
     */
    public void setRedirect(final URI redirect) {
        this.redirect = redirect;
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
