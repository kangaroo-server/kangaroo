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

package net.krotscheck.kangaroo.authz.oauth2.session;

import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.oauth2.session.tasks.HttpSessionCleanupTask;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.hibernate.HibernateFeature;
import net.krotscheck.kangaroo.common.timedtasks.RepeatingTask;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.annotations.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Test that the session feature works.
 *
 * @author Michael Krotscheck
 */
public final class SessionFeatureTest extends ContainerTest {

    /**
     * Create an application.
     *
     * @return The application to test.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(HibernateFeature.class);
        a.register(DatabaseFeature.class);
        a.register(ConfigurationFeature.class);
        a.register(SessionFeature.class);
        a.register(MockService.class);
        return a;
    }

    /**
     * Quick check to see if we can inject and access the authenticators. The
     * test code itself is in the mock service below.
     */
    @Test
    public void testStatus() {
        Response response = target("/").request().get();
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    /**
     * A simple endpoint that returns the list of authenticators registered.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * The HTTP session.
         */
        private Provider<HttpSession> httpSessionProvider;

        /**
         * The task provider.
         */
        private Provider<RepeatingTask> taskProvider;

        /**
         * The oauth2 token.
         */
        private OAuthToken token;

        /**
         * Create a new instance of the status service.
         *
         * @param httpSessionProvider The HTTP session provider.
         * @param taskProvider        Provider for injected tasks.
         * @param token               The OAUth token (should actually be null)
         */
        @Inject
        public MockService(final Provider<HttpSession> httpSessionProvider,
                           final Provider<RepeatingTask> taskProvider,
                           @Optional final OAuthToken token) {
            this.taskProvider = taskProvider;
            this.httpSessionProvider = httpSessionProvider;
            this.token = token;
        }

        /**
         * Try to access various injectees.
         *
         * @return HTTP Response object with the current service status.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response status() {
            HttpSession session = httpSessionProvider.get();
            if (session == null || token != null) {
                return Response.status(Status.BAD_REQUEST).build();
            }

            RepeatingTask task = taskProvider.get();
            if (task == null || !(task instanceof HttpSessionCleanupTask)) {
                return Response.status(Status.BAD_REQUEST).build();
            }

            return Response
                    .status(HttpStatus.SC_OK)
                    .build();
        }
    }
}
