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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModelProperty;
import net.krotscheck.kangaroo.common.hibernate.id.AbstractEntityReferenceDeserializer;
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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
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
@Table(name = "roles")
@Indexed(index = "roles")
@Analyzer(definition = "entity_analyzer")
public final class Role extends AbstractAuthzEntity {

    /**
     * User record to whom this application belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id")
    @JsonDeserialize(using = Application.Deserializer.class)
    @IndexedEmbedded(includePaths = {"id", "owner.id"})
    @ApiModelProperty(
            required = true,
            dataType = "string",
            example = "3f631a2d6a04f5cc55f9e192f45649b7"
    )
    private Application application;

    /**
     * List of the users that have this role.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "role")
    @JsonIgnore
    @ContainedIn
    private List<User> users = new ArrayList<>();

    /**
     * The name of the role.
     */
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    @Size(min = 3, max = 255, message = "Role name must be between 3 "
            + "and 255 characters.")
    @NotNull(message = "A Role name is required")
    @ApiModelProperty(required = true)
    private String name;

    /**
     * List of the role's scopes.
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
    @MapKey(name = "name")
    @SortNatural
    private SortedMap<String, ApplicationScope> scopes = new TreeMap<>();

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
    public SortedMap<String, ApplicationScope> getScopes() {
        return scopes;
    }

    /**
     * Set this role's scopes.
     *
     * @param scopes A new list of scopes.
     */
    public void setScopes(final SortedMap<String, ApplicationScope> scopes) {
        this.scopes = new TreeMap<>(scopes);
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
        if (application != null) {
            return application.getOwner();
        }
        return null;
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
