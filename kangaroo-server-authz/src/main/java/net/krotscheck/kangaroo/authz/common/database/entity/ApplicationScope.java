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
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * The application scope, as defined by the user.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "application_scopes")
@Indexed(index = "application_scopes")
@Analyzer(definition = "entity_analyzer")
public final class ApplicationScope extends AbstractAuthzEntity {

    /**
     * The Application to whom this scope belongs.
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
     * All roles that are authorized to request this scope.
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "scopes")
    @JsonIgnore
    private List<Role> roles = new ArrayList<>();

    /**
     * All tokens that have access to this scope.
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "scopes")
    @JsonIgnore
    private List<OAuthToken> tokens = new ArrayList<>();

    /**
     * The string for this scope (used in OAuth exchange).
     */
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    @Size(min = 3, max = 255, message = "Scope name must be between 3 "
            + "and 255 characters.")
    @ApiModelProperty(required = true)
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
