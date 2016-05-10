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
import net.krotscheck.features.database.filters.UserFilter;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilterFactory;
import org.apache.lucene.analysis.miscellaneous.TrimFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
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
@Table(name = "applications")
@Indexed(index = "applications")
@FullTextFilterDefs({
        @FullTextFilterDef(name = "user",
                impl = UserFilter.class,
                cache = FilterCacheModeType.INSTANCE_ONLY),
})
@AnalyzerDef(name = "applicationsanalyzer",
        charFilters = {
                @CharFilterDef(factory = HTMLStripCharFilterFactory.class)
        },
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
        filters = {
                @TokenFilterDef(factory = TrimFilterFactory.class),
                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
                @TokenFilterDef(factory = StopFilterFactory.class),
                @TokenFilterDef(factory = SnowballPorterFilterFactory.class,
                        params = {
                                @Parameter(name = "language", value = "English")
                        }),
                @TokenFilterDef(
                        factory = RemoveDuplicatesTokenFilterFactory.class)
        })
public final class Application extends AbstractEntity {

    /**
     * List of the users in this application.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "application")
    @JsonIgnore
    private List<User> users;

    /**
     * List of the application's clients.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "application")
    @JsonIgnore
    private List<Client> clients;

    /**
     * List of the application's roles.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "application")
    @JsonIgnore
    private List<Role> roles;

    /**
     * List of the application's authenticators.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "application")
    @JsonIgnore
    private List<Authenticator> authenticators;

    /**
     * The owner of the application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner", nullable = true, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = User.Deserializer.class)
    private User owner;

    /**
     * The name of the application.
     */
    @Basic(optional = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    @Column(name = "name", nullable = false)
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
     * The list of authenticators active in this application.s
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
