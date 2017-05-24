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

package net.krotscheck.kangaroo.authz.common.database.entity;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.krotscheck.kangaroo.common.hibernate.deserializer.AbstractEntityReferenceDeserializer;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The application entity, representing an app which uses our system for
 * authentication.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "applications")
@Indexed(index = "applications")
@Analyzer(definition = "entity_analyzer")
public final class Application extends AbstractAuthzEntity {

    /**
     * List of the users in this application.
     */
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "application",
            cascade = {CascadeType.REMOVE},
            orphanRemoval = true
    )
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ContainedIn
    private List<User> users = new ArrayList<>();

    /**
     * List of the application's clients.
     */
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "application",
            cascade = {CascadeType.REMOVE},
            orphanRemoval = true
    )
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ContainedIn
    private List<Client> clients = new ArrayList<>();

    /**
     * List of the application's roles.
     */
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "application",
            cascade = {CascadeType.REMOVE},
            orphanRemoval = true
    )
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Role> roles = new ArrayList<>();

    /**
     * List of the application's scopes.
     */
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "application",
            cascade = {CascadeType.REMOVE},
            orphanRemoval = true
    )
    @JsonIgnore
    @MapKey(name = "name")
    @SortNatural
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ContainedIn
    private SortedMap<String, ApplicationScope> scopes = new TreeMap<>();

    /**
     * The owner of the application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner", nullable = true, updatable = true)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = User.Deserializer.class)
    @IndexedEmbedded(includePaths = "id")
    private User owner;

    /**
     * The default role for this application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defaultRole", nullable = true, updatable = true)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Role.Deserializer.class)
    @IndexedEmbedded(includePaths = "id")
    private Role defaultRole;

    /**
     * The name of the application.
     */
    @Basic(optional = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    @Column(name = "name", nullable = false)
    @Size(min = 3, max = 255, message = "Application name must be between 3 "
            + "and 255 characters.")
    private String name;

    /**
     * Get the name for this application.
     *
     * @return The application's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for this application.
     *
     * @param name A new name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get this application's clients.
     *
     * @return A list of clients.
     */
    public List<Client> getClients() {
        return clients;
    }

    /**
     * Set this application's clients.
     *
     * @param clients A new list of clients.
     */
    public void setClients(final List<Client> clients) {
        this.clients = new ArrayList<>(clients);
    }

    /**
     * Get this application's users.
     *
     * @return A list of users.
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * Set this application's users.
     *
     * @param users A new list of users.
     */
    public void setUsers(final List<User> users) {
        this.users = new ArrayList<>(users);
    }

    /**
     * Get this application's roles.
     *
     * @return A list of roles.
     */
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * Set this application's roles.
     *
     * @param roles A new list of roles.
     */
    public void setRoles(final List<Role> roles) {
        this.roles = new ArrayList<>(roles);
    }

    /**
     * Get this application's scopes.
     *
     * @return A list of scopes.
     */
    public SortedMap<String, ApplicationScope> getScopes() {
        return scopes;
    }

    /**
     * Set this application's scopes.
     *
     * @param scopes A new list of scopes.
     */
    public void setScopes(final SortedMap<String, ApplicationScope> scopes) {
        this.scopes = new TreeMap<>(scopes);
    }

    /**
     * The owner for this application.
     *
     * @return The current owner, or null if not set.
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Set the owner for this application.
     *
     * @param owner The new owner.
     */
    public void setOwner(final User owner) {
        this.owner = owner;
    }

    /**
     * Get the default role for this application.
     *
     * @return The default role.
     */
    public Role getDefaultRole() {
        return defaultRole;
    }

    /**
     * Set the default role for this application.
     *
     * @param defaultRole The default role.
     */
    public void setDefaultRole(final Role defaultRole) {
        this.defaultRole = defaultRole;
    }

    /**
     * Deserialize a reference to an Application.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<Application> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(Application.class);
        }
    }
}
