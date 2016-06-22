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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * The application scope, as defined by the user.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "application_scopes")
public final class ApplicationScope extends AbstractEntity {

    /**
     * The Application to whom this scope belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    private Application application;

    /**
     * All roles that are authorized to request this scope.
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "scopes")
    @JsonIgnore
    private List<Role> roles;

    /**
     * All tokens that have access to this scope.
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "scopes")
    @JsonIgnore
    private List<OAuthToken> tokens;

    /**
     * The string for this scope (used in OAuth exchange).
     */
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Get the application this scope belongs to.
     *
     * @return The application.
     */
    public Application getApplication() {
        return application;
    }

    /**
     * Set the application which this scope belongs to.
     *
     * @param application The new application.
     */
    public void setApplication(final Application application) {
        this.application = application;
    }

    /**
     * Get all roles associated with this scope.
     *
     * @return The list of roles.
     */
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * Set all roles associated with this scope.
     *
     * @param roles The roles.
     */
    public void setRoles(final List<Role> roles) {
        this.roles = new ArrayList<>(roles);
    }

    /**
     * Get all tokens associated with this scope.
     *
     * @return The list of tokens.
     */
    public List<OAuthToken> getTokens() {
        return tokens;
    }

    /**
     * Set all tokens associated with this scope.
     *
     * @param tokens The tokens.
     */
    public void setTokens(final List<OAuthToken> tokens) {
        this.tokens = new ArrayList<>(tokens);
    }

    /**
     * Get the name for this scope.
     *
     * @return The scope's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for this scope.
     *
     * @param name A new name.
     */
    public void setName(final String name) {
        this.name = name;
    }


    /**
     * Deserialize a reference to an Application.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<ApplicationScope> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(ApplicationScope.class);
        }
    }
}
