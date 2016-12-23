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

package net.krotscheck.kangaroo.common.hibernate;

import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import org.glassfish.jersey.server.ResourceConfig;
import net.krotscheck.kangaroo.test.KangarooJerseyTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.service.ServiceRegistry;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Test that all expected classes are in the hibernate feature.
 *
 * @author Michael Krotscheck
 */
public final class HibernateFeatureTest extends KangarooJerseyTest {

    /**
     * Ensure that the JNDI Resource exists.
     */
    @ClassRule
    public static final TestRule DATABASE = new DatabaseResource();

    /**
     * Run a service request.
     */
    @Test
    public void testService() {
        target("/test")
                .request(MediaType.APPLICATION_JSON)
                .get();
    }

    /**
     * Configure the application.
     *
     * @return A properly configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(HibernateFeature.class);
        config.register(TestService.class);

        return config;
    }

    /**
     * A test service that asserts our injection scopes.
     */
    @Path("/test")
    public static final class TestService {

        /**
         * Hibernate service registry.
         */
        private ServiceRegistry serviceRegistry;

        /**
         * Session factory.
         */
        private SessionFactory factory;

        /**
         * Search factory injection.
         */
        private SearchFactory searchFactory;

        /**
         * FullText session injector.
         */
        private FullTextSession ftSession;

        /**
         * Session injector.
         */
        private Session session;

        /**
         * Create a new instance of our test service.
         *
         * @param sr  ServiceRegistry
         * @param f   Session Factory
         * @param s   Hibernate Session
         * @param sF  Search Factory
         * @param ftS Full text session
         */
        @Inject
        public TestService(final ServiceRegistry sr,
                           final SessionFactory f,
                           final SearchFactory sF,
                           final Session s,
                           final FullTextSession ftS) {
            serviceRegistry = sr;
            factory = f;
            session = s;
            searchFactory = sF;
            ftSession = ftS;
        }

        /**
         * Run the test service.
         *
         * @return An OK response.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response testService() {
            Assert.assertNotNull(serviceRegistry);
            Assert.assertNotNull(searchFactory);
            Assert.assertNotNull(factory);
            Assert.assertNotNull(ftSession);
            Assert.assertNotNull(session);

            return Response.ok().build();
        }
    }

}
