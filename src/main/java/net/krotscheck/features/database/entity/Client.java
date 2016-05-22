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

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.features.database.deserializer.AbstractEntityReferenceDeserializer;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * This represents a registered client, as well as it's connection metadata,
 * for
 * a specific application. Multiple different clients may exist per
 * application.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "clients")
public final class Client extends AbstractEntity {

    /**
     * The Application to whom this client belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    private Application application;

    /**
     * OAuth tokens issued to this client.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
    @Cascade(CascadeType.ALL)
    @JsonIgnore
    private List<OAuthToken> tokens;

    /**
     * List of all authenticator states currently active.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
    @Cascade(CascadeType.ALL)
    @JsonIgnore
    private List<AuthenticatorState> states;

    /**
     * Human recognizable name for this client.
     */
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Human recognizable name for this client.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ClientType type = ClientType.Public;

    /**
     * The unique client ID by which the API client identifies itself. This is
     * matched against the permitted URL's.
     */
    @Basic(optional = false)
    @Column(name = "clientId", unique = true, updatable = false)
    private String clientId;

    /**
     * The base URL from which all authorization referrals must originate.
     */
    @Basic(optional = false)
    @Column(name = "referrer", nullable = false)
    private URL referrer;

    /**
     * The target redirection URL for all return requests.
     */
    @Basic(optional = false)
    @Column(name = "redirect", nullable = false)
    private URL redirect;

    /**
     * Expiration time for auth tokens, in seconds. Default one hour.
     */
    @Basic(optional = false)
    @Column(name = "authTokenExpire", nullable = false)
    private Integer authTokenExpire = 3600;

    /**
     * Expiration time for refresh tokens, in seconds. Default one week.
     */
    @Basic(optional = false)
    @Column(name = "refreshTokenExpire", nullable = false)
    private Integer refreshTokenExpire = 604800;

    /**
     * Get the application this client belongs to.
     *
     * @return The application.
     */
    public Application getApplication() {
        return application;
    }

    /**
     * Set the application which this client belongs to.
     *
     * @param application The new application.
     */
    public void setApplication(final Application application) {
        this.application = application;
    }

    /**
     * Get the name for this client.
     *
     * @return The client's human-readable name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the client's name.
     *
     * @param name The name to use, should be human-readable.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the client ID.
     *
     * @return The client ID.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Set the client ID.
     *
     * @param clientId The new client ID.
     */
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /**
     * Get the valid referrer for this client.
     *
     * @return The referrer URI.
     */
    public URL getReferrer() {
        return referrer;
    }

    /**
     * Set the referrer URL.
     *
     * @param referrer A new referrer URL.
     */
    public void setReferrer(final URL referrer) {
        this.referrer = referrer;
    }

    /**
     * The eventual redirect location for the client. Must match what the
     * client
     * provides.
     *
     * @return The new redirect URI.
     */
    public URL getRedirect() {
        return redirect;
    }

    /**
     * Set a new redirect URI.
     *
     * @param redirect The new URI.
     */
    public void setRedirect(final URL redirect) {
        this.redirect = redirect;
    }

    /**
     * Get the Auth token expiration time, in seconds.
     *
     * @return The time in seconds.
     */
    public Integer getAuthTokenExpire() {
        return authTokenExpire;
    }

    /**
     * Set the new timeout for the auth token.
     *
     * @param authTokenExpire New timeout, in seconds.
     */
    public void setAuthTokenExpire(final Integer authTokenExpire) {
        this.authTokenExpire = authTokenExpire;
    }

    /**
     * Get the expiration time of the refresh token.
     *
     * @return Expiration time, in seconds.
     */
    public Integer getRefreshTokenExpire() {
        return refreshTokenExpire;
    }

    /**
     * Set a new expiration time for the refresh token.
     *
     * @param refreshTokenExpire New expiration, in seconds.
     */
    public void setRefreshTokenExpire(final Integer refreshTokenExpire) {
        this.refreshTokenExpire = refreshTokenExpire;
    }

    /**
     * Get all the tokens issued under this client.
     *
     * @return A list of all tokens (lazy loaded)
     */
    public List<OAuthToken> getTokens() {
        return tokens;
    }

    /**
     * Set the list of tokens for this client.
     *
     * @param tokens A new list of tokens.
     */
    public void setTokens(final List<OAuthToken> tokens) {
        this.tokens = new ArrayList<>(tokens);
    }

    /**
     * Get the list of currently active query states.
     *
     * @return The list of states.
     */
    public List<AuthenticatorState> getStates() {
        return states;
    }

    /**
     * Set the list of states.
     *
     * @param states The list of states.
     */
    public void setStates(final List<AuthenticatorState> states) {
        this.states = new ArrayList<>(states);
    }

    /**
     * Get the client type.
     *
     * @return The client type.
     */
    public ClientType getType() {
        return type;
    }

    /**
     * Set the client type.
     *
     * @param type The client type!
     */
    public void setType(final ClientType type) {
        this.type = type;
    }

    /**
     * Deserialize a reference to an User.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<Client> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(Client.class);
        }
    }
}
