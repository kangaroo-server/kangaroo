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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.krotscheck.features.database.deserializer.AbstractEntityReferenceDeserializer;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * This entity describes state data stored while an authorization request has
 * been passed to the authentication authenticator. It is retrieved based on the
 * authenticatorState and the authenticatorNonce.
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
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Client.Deserializer.class)
    private Client client;

    /**
     * The authenticator for this state.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authenticator", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Authenticator.Deserializer.class)
    private Authenticator authenticator;

    /**
     * The stored request state for the authenticator.
     */
    @Basic(optional = false)
    @Column(name = "authenticatorState", unique = false)
    private String authenticatorState;

    /**
     * The stored request nonce for the authenticator.
     */
    @Basic(optional = false)
    @Column(name = "authenticatorNonce", unique = false)
    private String authenticatorNonce;

    /**
     * The client's state.
     */
    @Basic(optional = false)
    @Column(name = "clientState", unique = false)
    private String clientState;

    /**
     * The scope requested from the client.
     */
    @Basic(optional = false)
    @Column(name = "clientScope", unique = false)
    private String clientScope;

    /**
     * The nonce attached to the client's request.
     */
    @Basic(optional = false)
    @Column(name = "clientNonce", unique = false)
    private String clientNonce;

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
     * Retrieve the state passed to the authenticator.
     *
     * @return The state which the authenticator received.
     */
    public String getAuthenticatorState() {
        return authenticatorState;
    }

    /**
     * Set a new authenticator state.
     *
     * @param authenticatorState A new auth state.
     */
    public void setAuthenticatorState(final String authenticatorState) {
        this.authenticatorState = authenticatorState;
    }

    /**
     * Get the cryptographic nonce for the authentication request.
     *
     * @return The nonce.
     */
    public String getAuthenticatorNonce() {
        return authenticatorNonce;
    }

    /**
     * Set an authentication nonce.
     *
     * @param authenticatorNonce A cryptographic nonce.
     */
    public void setAuthenticatorNonce(final String authenticatorNonce) {
        this.authenticatorNonce = authenticatorNonce;
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

    /**
     * Retrieve the client scope. Note that this is the raw value, and may
     * require some additional processing to determine the true value.
     *
     * @return The requested client scope.
     */
    public String getClientScope() {
        return clientScope;
    }

    /**
     * Set a raw client scope.
     *
     * @param clientScope The scope requested by the client.
     */
    public void setClientScope(final String clientScope) {
        this.clientScope = clientScope;
    }

    /**
     * Retrieve the client nonce.
     *
     * @return The client's nonce.
     */
    public String getClientNonce() {
        return clientNonce;
    }

    /**
     * Set the client's nonce.
     *
     * @param clientNonce The client's nonce.
     */
    public void setClientNonce(final String clientNonce) {
        this.clientNonce = clientNonce;
    }

    /**
     * Deserialize a reference to an Authenticator.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<AuthenticatorState> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(AuthenticatorState.class);
        }
    }
}
