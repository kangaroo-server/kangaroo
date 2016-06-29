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

package net.krotscheck.kangaroo.database.entity;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.kangaroo.database.deserializer.AbstractEntityReferenceDeserializer;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
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
     * List of the application's authenticators.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
    @JsonIgnore
    private List<Authenticator> authenticators;

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
    private ClientType type = ClientType.AuthorizationGrant;

    /**
     * A client secret, indicating a password which must be provided to the
     * client.
     */
    @Basic
    @Column(name = "clientSecret", updatable = true, nullable = true)
    private String clientSecret;

    /**
     * A collection of referral URL's, used for CORS matching.
     */
    @ElementCollection
    @CollectionTable(name = "client_referrers",
            joinColumns = @JoinColumn(name = "client"))
    @Column(name = "referrer")
    private Set<URI> referrers;

    /**
     * A list of redirect URL's, used for redirection-based flows.
     */
    @ElementCollection
    @CollectionTable(name = "client_redirects",
            joinColumns = @JoinColumn(name = "client"))
    @Column(name = "redirect")
    private Set<URI> redirects;

    /**
     * The configuration settings for this application.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_configs",
            joinColumns = @JoinColumn(name = "client"))
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @Cascade(CascadeType.ALL)
    private Map<String, String> configuration;

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
     * Retrieve the client secret.
     *
     * @return The client secret.
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Set the client secret.
     *
     * @param clientSecret A new client secret.
     */
    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Get the list of valid referrers.
     *
     * @return This client's list of valid referrers.
     */
    public Set<URI> getReferrers() {
        return referrers;
    }

    /**
     * Update the set of valid referrers.
     *
     * @param referrers A new set of referrers.
     */
    public void setReferrers(final Set<URI> referrers) {
        this.referrers = referrers;
    }

    /**
     * Get the valid redirect URI's.
     *
     * @return The valid redirect url's.
     */
    public Set<URI> getRedirects() {
        return redirects;
    }

    /**
     * Set the list of redirects.
     *
     * @param redirects The redirects.
     */
    public void setRedirects(final Set<URI> redirects) {
        this.redirects = redirects;
    }

    /**
     * The list of authenticators active in this client.
     *
     * @return The list of authenticators.
     */
    public List<Authenticator> getAuthenticators() {
        return authenticators;
    }

    /**
     * Set the authenticators.
     *
     * @param authenticators New list of authenticators.
     */
    public void setAuthenticators(final List<Authenticator> authenticators) {
        this.authenticators = new ArrayList<>(authenticators);
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
     * Retrieve configuration for this client.
     *
     * @return A set of configuration elements, such as token expiry.
     */
    public Map<String, String> getConfiguration() {
        return configuration;
    }

    /**
     * Set the configuration for this client.
     *
     * @param configuration The new configuration.
     */
    public void setConfiguration(final Map<String, String> configuration) {
        this.configuration = new HashMap<>(configuration);
    }

    /**
     * Extract the authorization code expiration time (in seconds) for this
     * client. This value is derived from the default configuration values, or
     * from a default, if such exists.
     *
     * @return The expiration horizon of an access token, in seconds.
     */
    @JsonIgnore
    public Integer getAuthorizationCodeExpiresIn() {
        try {
            Map<String, String> config = getConfiguration();
            String value =
                    config.get(ClientConfig.AUTHORIZATION_CODE_EXPIRES_NAME);
            return Integer.parseInt(value);
        } catch (Exception e) {
            return ClientConfig.AUTHORIZATION_CODE_EXPIRES_DEFAULT;
        }
    }

    /**
     * Extract the access token expiration time (in seconds) for this client.
     * This value is derived from the default configuration values, or from a
     * default, if such exists.
     *
     * @return The expiration horizon of an access token, in seconds.
     */
    @JsonIgnore
    public Integer getAccessTokenExpireIn() {
        try {
            Map<String, String> config = getConfiguration();
            String value = config.get(ClientConfig.ACCESS_TOKEN_EXPIRES_NAME);
            return Integer.parseInt(value);
        } catch (Exception e) {
            return ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT;
        }
    }

    /**
     * Extract the refresh token expiration time (in seconds) for this client.
     * This value is derived from the default configuration values, or from a
     * default, if such exists.
     *
     * @return The expiration horizon of an access token, in seconds.
     */
    @JsonIgnore
    public Integer getRefreshTokenExpireIn() {
        try {
            Map<String, String> config = getConfiguration();
            String value = config.get(ClientConfig.REFRESH_TOKEN_EXPIRES_NAME);
            return Integer.parseInt(value);
        } catch (Exception e) {
            return ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT;
        }
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
