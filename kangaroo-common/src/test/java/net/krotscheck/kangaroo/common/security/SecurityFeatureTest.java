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

package net.krotscheck.kangaroo.common.security;

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.test.KangarooJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Assert that our security features are injected.
 *
 * @author Michael Krotscheck
 */
public class SecurityFeatureTest extends KangarooJerseyTest {

    /**
     * Build an application.
     *
     * @return A configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(SecurityFeature.class);
        config.register(MockService.class);
        return config;
    }

    /**
     * Assert that the jackson feature is available.
     */
    @Test
    public void testFilters() {
        Response r = target("/").request().get();

        Assert.assertEquals("Deny",
                r.getHeaderString(HttpHeaders.X_FRAME_OPTIONS));
    }

    /**
     * A simple endpoint.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * Return OK.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response modifyPojo() {
            return Response.ok().build();
        }

    }
}
