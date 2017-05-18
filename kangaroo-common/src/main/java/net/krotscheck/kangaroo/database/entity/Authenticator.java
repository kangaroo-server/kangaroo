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
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.krotscheck.kangaroo.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.database.deserializer.AbstractEntityReferenceDeserializer;
import net.krotscheck.kangaroo.database.jackson.Views;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.EnumBridge;

import javax.persistence.CascadeType;
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
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An authentication provider, linked to an application, containing
 * miscellaneous configuration information for this provider.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "authenticators")
@Indexed(index = "authenticators")
@Analyzer(definition = "entity_analyzer")
public final class Authenticator extends AbstractEntity {

    /**
     * The Client to whom this authenticator belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Client.Deserializer.class)
    @IndexedEmbedded(includePaths = {"id", "application.owner.id", "name"})
    private Client client;

    /**
     * List of all authenticator states currently active.
     */
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "authenticator",
            cascade = {CascadeType.REMOVE},
            orphanRemoval = true
    )
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<AuthenticatorState> states = new ArrayList<>();

    /**
     * The authenticator type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO,
            bridge = @FieldBridge(impl = EnumBridge.class))
    @NotNull
    @JsonView(Views.Public.class)
    private AuthenticatorType type;

    /**
     * Configuration data for this authenticator, different for each one.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "authenticator_params",
            joinColumns = @JoinColumn(name = "authenticator"))
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private Map<String, String> configuration = new HashMap<>();

    /**
     * Get the client for this authenticator.
     *
     * @return The authenticator's client.
     */
    public Client getClient() {
        return client;
    }

    /**
     * Set a new client for this authenticator.
     *
     * @param client The new client.
     */
    public void setClient(final Client client) {
        this.client = client;
    }

    /**
     * Get the authenticator type, a string lookup key by which the
     * authenticator is retrieved from the service locator.
     *
     * @return The type.
     */
    public AuthenticatorType getType() {
        return type;
    }

    /**
     * Set a new authenticator type.
     *
     * @param type The new type. Must match one of the IAuthenticator injection
     *             names, else will throw scope exceptions.
     */
    public void setType(final AuthenticatorType type) {
        this.type = type;
    }

    /**
     * Get the current configuration, as persisted in the database.
     *
     * @return The configuration.
     */
    public Map<String, String> getConfiguration() {
        return configuration;
    }

    /**
     * Set the configuration values.
     *
     * @param configuration The new config values.
     */
    public void setConfiguration(final Map<String, String> configuration) {
        this.configuration = configuration;
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
     * Deserialize a reference to an Authenticator.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<Authenticator> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(Authenticator.class);
        }
    }
}
