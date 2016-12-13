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
 *
 */

package net.krotscheck.kangaroo.database.entity;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.krotscheck.kangaroo.database.deserializer.AbstractEntityReferenceDeserializer;
import net.krotscheck.kangaroo.database.filters.UUIDFilter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
import javax.persistence.Transient;
import java.net.URI;
import java.security.Principal;
import java.util.Calendar;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This entity represents an OAuth Token, provided to a user, for a specific
 * application, on a particular client's domain.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "oauth_tokens")
@Indexed(index = "oauth_tokens")
@Analyzer(definition = "entity_analyzer")
@FullTextFilterDefs({
        @FullTextFilterDef(name = "uuid_token_owner",
                impl = UUIDFilter.class,
                cache = FilterCacheModeType.INSTANCE_ONLY),
        @FullTextFilterDef(name = "uuid_token_client",
                impl = UUIDFilter.class,
                cache = FilterCacheModeType.INSTANCE_ONLY),
        @FullTextFilterDef(name = "uuid_token_user",
                impl = UUIDFilter.class,
                cache = FilterCacheModeType.INSTANCE_ONLY),
        @FullTextFilterDef(name = "uuid_token_identity",
                impl = UUIDFilter.class,
                cache = FilterCacheModeType.INSTANCE_ONLY)
})
public final class OAuthToken extends AbstractEntity implements Principal {

    /**
     * The authenticated user identity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identity", updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = UserIdentity.Deserializer.class)
    @IndexedEmbedded(includePaths = {"id", "user.id"})
    private UserIdentity identity;

    /**
     * Client for which this is valid.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Client.Deserializer.class)
    @IndexedEmbedded(includePaths = {"id", "application.owner.id"})
    private Client client;

    /**
     * The parent auth token (used for refresh tokens only).
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.REMOVE, CascadeType.MERGE}
    )
    @JoinColumn(name = "authToken", nullable = true, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = OAuthToken.Deserializer.class)
    private OAuthToken authToken;

    /**
     * The token type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tokenType", nullable = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    private OAuthTokenType tokenType;

    /**
     * Expires in how many seconds?
     */
    @Basic(optional = false)
    @Column(name = "expiresIn", nullable = false)
    private Long expiresIn;

    /**
     * Authorization Codes must keep track of the redirect they were issued
     * for.
     */
    @Basic
    @Column(name = "redirect", nullable = true)
    @Type(type = "net.krotscheck.kangaroo.database.type.URIType")
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
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
    @MapKey(name = "name")
    @SortNatural
    private SortedMap<String, ApplicationScope> scopes = new TreeMap<>();

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
    public Long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Set the expiration time, in seconds, from the creation date.
     *
     * @param expiresIn The time, in seconds.
     */
    public void setExpiresIn(final Number expiresIn) {
        if (expiresIn == null) {
            this.expiresIn = null;
        } else {
            this.expiresIn = expiresIn.longValue();
        }
    }

    /**
     * Set the expiration time, in seconds, from the creation date.
     *
     * @param expiresIn The time, in seconds.
     */
    public void setExpiresIn(final int expiresIn) {
        this.expiresIn = (long) expiresIn;
    }

    /**
     * Set the expiration time, in seconds, from the creation date.
     *
     * @param expiresIn The time, in seconds.
     */
    @JsonSetter
    public void setExpiresIn(final long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Get this token's scopes.
     *
     * @return A map of scopes.
     */
    @JsonIgnore
    public SortedMap<String, ApplicationScope> getScopes() {
        return scopes;
    }

    /**
     * Set this token's scopes.
     *
     * @param scopes A new map of scopes.
     */
    @JsonIgnore
    public void setScopes(final SortedMap<String, ApplicationScope> scopes) {
        this.scopes = new TreeMap<>(scopes);
    }

    /**
     * This method returns true if the created time, plus the expiration
     * seconds, is less than the current time.
     *
     * @return True of this token is expired, otherwise false.
     */
    @Transient
    @JsonIgnore
    public boolean isExpired() {
        if (getCreatedDate() == null) {
            return true;
        }

        Calendar now = Calendar.getInstance();
        Calendar expireDate = (Calendar) getCreatedDate().clone();
        expireDate.add(Calendar.SECOND, getExpiresIn().intValue());
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
     * Returns the name of this principal. In our case, this will either be
     * the remote ID of the user identity, or the human readable name of the
     * OAuth Client application (in the case of a ClientCredentials Client).
     *
     * @return the name of this principal.
     */
    @Override
    @Transient
    @JsonIgnore
    public String getName() {
        if (getClient().getType().equals(ClientType.ClientCredentials)) {
            return getClient().getName();
        } else {
            return getIdentity().getRemoteId();
        }
    }

    /**
     * The owner of this entity.
     *
     * @return This entity's owner, if it exists.
     */
    @Override
    @Transient
    @JsonIgnore
    public User getOwner() {
        if (client != null) {
            return client.getOwner();
        }
        return null;
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
