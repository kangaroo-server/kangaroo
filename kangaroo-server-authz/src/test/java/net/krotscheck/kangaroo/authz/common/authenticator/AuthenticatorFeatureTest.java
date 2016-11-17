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

package net.krotscheck.kangaroo.authz.common.authenticator;

import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.common.hibernate.HibernateFeature;
import net.krotscheck.kangaroo.test.ContainerTest;
import org.apache.http.HttpStatus;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Unit tests for the authenticator feature.
 *
 * @author Michael Krotscheck
 */
public final class AuthenticatorFeatureTest extends ContainerTest {

    /**
     * Create an application.
     *
     * @return The application to test.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(DatabaseFeature.class);
        a.register(HibernateFeature.class);
        a.register(AuthenticatorFeature.class);
        a.register(AuthenticatorFeatureTest.MockService.class);
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
         * The system configuration from which to read status features.
         */
        private ServiceLocator locator;

        /**
         * Create a new instance of the status service.
         *
         * @param locator Injected system locator.
         */
        @Inject
        public MockService(final ServiceLocator locator) {
            this.locator = locator;
        }

        /**
         * Always returns the version.
         *
         * @return HTTP Response object with the current service status.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response status() {

            // Make sure all the enums are available.
            for (AuthenticatorType t : AuthenticatorType.values()) {
                IAuthenticator authenticator =
                        locator.getService(IAuthenticator.class, t.name());
                Assert.assertNotNull(authenticator);
            }

            // Make sure there aren't any zombies.
            List<IAuthenticator> authenticators =
                    locator.getAllServices(IAuthenticator.class);
            Assert.assertEquals(AuthenticatorType.values().length,
                    authenticators.size());

            return Response
                    .status(HttpStatus.SC_OK)
                    .build();
        }
    }
}
