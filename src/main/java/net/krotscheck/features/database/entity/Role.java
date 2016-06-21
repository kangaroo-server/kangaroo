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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.krotscheck.features.database.deserializer.AbstractEntityReferenceDeserializer;
import org.hibernate.annotations.Cascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * The application entity, representing an app which uses our system for
 * authentication.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "roles")
public final class Role extends AbstractEntity {

    /**
     * User record to whom this application belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Application.Deserializer.class)
    private Application application;

    /**
     * List of the users that have this role.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "role")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @JsonIgnore
    private List<User> users;

    /**
     * The name of the role.
     */
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * List of the application's scopes.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_scopes",
            joinColumns = {
                    @JoinColumn(name = "role",
                            nullable = false, updatable = false)},
            inverseJoinColumns = {
                    @JoinColumn(name = "scope",
                            nullable = false, updatable = false)})
    @JsonIgnore
    private List<ApplicationScope> scopes;

    /**
     * Get the application.
     *
     * @return The application for this role.
     */
    public Application getApplication() {
        return application;
    }

    /**
     * Set the application for this role.
     *
     * @param application A new application.
     */
    public void setApplication(final Application application) {
        this.application = application;
    }

    /**
     * Get the name for this role.
     *
     * @return The role name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for this role.
     *
     * @param name The new role name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the users that have this role.
     *
     * @return A list of users.
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * Set the list of users that have this role.
     *
     * @param users A new list of users.
     */
    public void setUsers(final List<User> users) {
        this.users = new ArrayList<>(users);
    }

    /**
     * Get this role's scopes.
     *
     * @return A list of scopes.
     */
    public List<ApplicationScope> getScopes() {
        return scopes;
    }

    /**
     * Set this role's scopes.
     *
     * @param scopes A new list of scopes.
     */
    public void setScopes(final List<ApplicationScope> scopes) {
        this.scopes = new ArrayList<>(scopes);
    }

    /**
     * Deserialize a reference to an Role.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<Role> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(Role.class);
        }
    }
}
