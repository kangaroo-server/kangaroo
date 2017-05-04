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

package net.krotscheck.kangaroo.common.logging;

import net.krotscheck.kangaroo.test.KangarooJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Unit tests for the logging feature. This test is really for coverage of
 * the injector only.
 *
 * @author Michael Krotscheck
 */
public final class LoggingFeatureTest extends KangarooJerseyTest {

    /**
     * Build the configured application.
     *
     * @return The configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(LoggingFeature.class);
        a.register(MockService.class);
        return a;
    }

    /**
     * Quick check to see if we can inject and access the configuration.
     */
    @Test
    public void testStatus() {
        Boolean response = target("/").request().get(Boolean.class);
        Assert.assertTrue(response);
    }

    /**
     * A simple endpoint that returns the system status.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * Always returns the version.
         *
         * @return HTTP Response object with the current service status.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response status() {
            return Response
                    .status(Status.OK)
                    .entity(SLF4JBridgeHandler.isInstalled())
                    .build();
        }
    }
}
