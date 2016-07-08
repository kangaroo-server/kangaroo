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

package net.krotscheck.kangaroo.database;

import net.krotscheck.kangaroo.database.listener.CreatedUpdatedListener;
import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.IFixture;
import org.apache.http.HttpStatus;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Unit tests for the database feature.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseFeatureTest extends ContainerTest {

    /**
     * Setup an application.
     *
     * @return A configured application.
     */
    @Override
    protected Application configure() {
        ResourceConfig a = new ResourceConfig();
        a.register(DatabaseFeature.class);
        a.register(MockService.class);
        return a;
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        return null;
    }

    /**
     * Quick check to see if we can inject the various components of the
     * database feature.
     */
    @Test
    public void testStatus() {
        String response = target("/").request().get(String.class);
        Assert.assertEquals("true", response);
    }

    /**
     * A simple endpoint that returns the system status.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * The system configuration from which to read status features.
         */
        private ServiceLocator serviceLocator;

        /**
         * Create a new instance of the status service.
         *
         * @param serviceLocator Service locator.
         */
        @Inject
        public MockService(final ServiceLocator serviceLocator) {
            this.serviceLocator = serviceLocator;
        }

        /**
         * Always returns the version.
         *
         * @return HTTP Response object with the current service status.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response status() {
            List<PreInsertEventListener> insertListeners = serviceLocator
                    .getAllServices(PreInsertEventListener.class);
            List<PreUpdateEventListener> updateListeners = serviceLocator
                    .getAllServices(PreUpdateEventListener.class);

            Assert.assertEquals(1, insertListeners.size());
            Assert.assertEquals(1, updateListeners.size());
            Assert.assertTrue(CreatedUpdatedListener.class
                    .isInstance(insertListeners.get(0)));
            Assert.assertTrue(CreatedUpdatedListener.class
                    .isInstance(updateListeners.get(0)));

            return Response
                    .status(HttpStatus.SC_OK)
                    .entity(true)
                    .build();
        }
    }
}
