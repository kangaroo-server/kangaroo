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

package net.krotscheck.kangaroo.common.config;

import net.krotscheck.kangaroo.test.jersey.KangarooJerseyTest;
import org.apache.commons.configuration.MapConfiguration;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Properties;

/**
 * Unit tests for the configuration binder.
 */
public class ConfigurationBinderTest extends KangarooJerseyTest {

    /**
     * Build the configured application.
     *
     * @return The configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        Properties properties = new Properties();
        properties.setProperty("foo", "bar");
        MapConfiguration mapConfig = new MapConfiguration(properties);

        ResourceConfig a = new ResourceConfig();
        a.register(ConfigurationFeature.class);
        a.register(new ConfigurationBinder(mapConfig));
        a.register(MockService.class);
        return a;
    }

    /**
     * Quick check to see if we can inject and access the configuration.
     */
    @Test
    public void testBoundExternalValue() {
        String response = target("/foo").request().get(String.class);
        Assert.assertEquals("bar", response);
    }

    /**
     * A simple endpoint that returns configuration values.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * The system configuration from which to read status features.
         */
        private SystemConfiguration config;

        /**
         * Create a new instance of the status service.
         *
         * @param config Injected system configuration.
         */
        @Inject
        public MockService(final SystemConfiguration config) {
            this.config = config;
        }

        /**
         * Return the value of a specific key.
         *
         * @param key The key value to return.
         * @return HTTP Response object with the current service status.
         */
        @GET
        @Path("{key}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getSpecificValue(@PathParam("key") final String key) {
            return Response
                    .status(Status.OK)
                    .entity(this.config.getString(key))
                    .build();
        }
    }
}
