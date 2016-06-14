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

import org.hibernate.annotations.SortNatural;

import java.net.URI;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

/**
 * This entity describes state data stored while an authorization request has
 * been passed to the authentication authenticator. It is retrieved based on
 * the authenticatorState and the authenticatorNonce.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "authenticator_states")
public final class AuthenticatorState extends AbstractEntity {

    /**
     * Domain on which this is valid.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client", nullable = false, updatable = false)
    private Client client;

    /**
     * The authenticator for this state.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authenticator", nullable = false, updatable = false)
    private Authenticator authenticator;

    /**
     * The client's state.
     */
    @Basic
    @Column(name = "clientState", unique = false)
    private String clientState;

    /**
     * The client's redirect.
     */
    @Basic(optional = false)
    @Column(name = "clientRedirect", unique = false)
    private URI clientRedirect;

    /**
     * List of the requested client scopes.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "authenticator_state_scopes",
            joinColumns = {
                    @JoinColumn(name = "authenticator_state",
                            nullable = false, updatable = false)},
            inverseJoinColumns = {
                    @JoinColumn(name = "scope",
                            nullable = false, updatable = false)})
    @MapKey(name = "name")
    @SortNatural
    private SortedMap<String, ApplicationScope> clientScopes;

    /**
     * Intermediate storage cache for data needed when the request comes back.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "authenticator_state_params",
            joinColumns = @JoinColumn(name = "authenticator_state"))
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private Map<String, String> authenticatorStateParams;

    /**
     * Retrieve the client.
     *
     * @return This state's client.
     */
    public Client getClient() {
        return client;
    }

    /**
     * Set the client.
     *
     * @param client The client for this state.
     */
    public void setClient(final Client client) {
        this.client = client;
    }

    /**
     * Get the authenticator for this state.
     *
     * @return This state's authenticator.
     */
    public Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * Set a new authenticator.
     *
     * @param authenticator The new authenticator.
     */
    public void setAuthenticator(final Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Get the validated client redirection URI.
     *
     * @return The URI which the client requested a result response to.
     */
    public URI getClientRedirect() {
        return clientRedirect;
    }

    /**
     * Set a new redirection URI.
     *
     * @param clientRedirect The redirection URI.
     */
    public void setClientRedirect(final URI clientRedirect) {
        this.clientRedirect = clientRedirect;
    }

    /**
     * The list of requested application scopes.
     *
     * @return The list of scopes.
     */
    public SortedMap<String, ApplicationScope> getClientScopes() {
        return clientScopes;
    }

    /**
     * Set the list of client scopes.
     *
     * @param clientScopes A new list of scopes.
     */
    public void setClientScopes(
            final SortedMap<String, ApplicationScope> clientScopes) {
        this.clientScopes = new TreeMap<>(clientScopes);
    }

    /**
     * Get the list of parameters persisted by the authenticator.
     *
     * @return The list of parameters.
     */
    public Map<String, String> getAuthenticatorStateParams() {
        return authenticatorStateParams;
    }

    /**
     * Persist a list of state parameters for this authenticator.
     *
     * @param authenticatorStateParams A new list of parameters.
     */
    public void setAuthenticatorStateParams(
            final Map<String, String> authenticatorStateParams) {
        this.authenticatorStateParams = new TreeMap<>(authenticatorStateParams);
    }

    /**
     * Retreive the state received from the client.
     *
     * @return The client's state.
     */
    public String getClientState() {
        return clientState;
    }

    /**
     * Store the state received from the client.
     *
     * @param clientState A new client state.
     */
    public void setClientState(final String clientState) {
        this.clientState = clientState;
    }
}
