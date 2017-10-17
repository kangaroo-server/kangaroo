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

package net.krotscheck.kangaroo.common.httpClient;

import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Test that the client feature works.
 *
 * @author Michael Krotscheck
 */
public class HttpClientFeatureTest extends ContainerTest {

    /**
     * Create an application.
     *
     * @return The application to test.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(HttpClientFeature.class);
        a.register(MockService.class);
        return a;
    }

    /**
     * Quick check to make sure that the code inside the service executes
     * correctly.
     */
    @Test
    public void testFeature() {
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
         * The injected component.
         */
        private Client client;

        /**
         * Create a new instance of the status service.
         *
         * @param client Injected HTTP Client.
         */
        @Inject
        public MockService(final Client client) {
            this.client = client;
        }

        /**
         * Test the service.
         *
         * @return HTTP Response object, OK if the test passes.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response test() {
            return client.target("https://www.example.com/")
                    .request()
                    .get();
        }
    }
}
