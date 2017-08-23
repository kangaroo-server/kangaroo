/*
 * Copyright (c) 2017 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.admin.v1.resource;

import net.krotscheck.kangaroo.authz.admin.v1.servlet.Config;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import org.apache.commons.configuration.Configuration;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This resource exposes a read-only service, with no authentication, that
 * lists out configuration settings for client applications. Note that no secure
 * piece of data should be exposed this way, as it is world-readable.
 *
 * @author Michael Krotscheck
 */
@Path("/config")
public final class ConfigService {

    /**
     * Servlet Configuration.
     */
    private final Configuration config;

    /**
     * Hibernate session.
     */
    private final Session session;

    /**
     * The configuration service.
     *
     * @param config  Configuration, injected.
     * @param session Session, injected.
     */
    @Inject
    public ConfigService(
            @Named(ServletConfigFactory.GROUP_NAME) final Configuration config,
            final Session session) {
        this.session = session;
        this.config = config;
    }

    /**
     * Return the system configuration.
     *
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings({"CPD-START"})
    public Response readConfiguration() {
        // Read the needed ID's...
        UUID applicationId = UUID.fromString(this.config
                .getString(Config.APPLICATION_ID));
        UUID clientId = UUID.fromString(this.config
                .getString(Config.APPLICATION_CLIENT_ID));

        // Load all the scopes.
        session.beginTransaction();

        Application app = session.get(Application.class, applicationId);

        Criteria criteria = session.createCriteria(ApplicationScope.class);
        criteria.add(Restrictions.eq("application", app));
        List<String> scopes = ((List<ApplicationScope>) criteria.list())
                .stream()
                .map(ApplicationScope::getName)
                .collect(Collectors.toList());

        session.getTransaction().commit();

        ConfigurationEntity e = new ConfigurationEntity();
        e.setClient(clientId);
        e.setScopes(scopes);
        return Response.ok(e).build();
    }

    /**
     * The pojo response object for this service.
     */
    public static final class ConfigurationEntity {

        /**
         * The ID of the web-ui admin client.
         */
        private UUID client;

        /**
         * Available scopes.
         */
        private List<String> scopes;

        /**
         * Get the available, configured scopes.
         *
         * @return The list of available scopes.
         */
        public List<String> getScopes() {
            return scopes;
        }

        /**
         * Set the list of scopes.
         *
         * @param scopes Set the new list of scopes.
         */
        public void setScopes(final List<String> scopes) {
            this.scopes = scopes;
        }

        /**
         * Retrieve the client id.
         *
         * @return The client ID.
         */
        public UUID getClient() {
            return client;
        }

        /**
         * Set a new client ID.
         *
         * @param client The client ID.
         */
        public void setClient(final UUID client) {
            this.client = client;
        }
    }
}
