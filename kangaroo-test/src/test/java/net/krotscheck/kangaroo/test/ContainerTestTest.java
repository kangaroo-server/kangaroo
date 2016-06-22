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

package net.krotscheck.kangaroo.test;

import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Test the container test, by implementing the container test.
 *
 * @author Michael Krotscheck
 */
public final class ContainerTestTest extends ContainerTest {

    /**
     * A blank dummy app.
     *
     * @return A dummy app!
     */
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        ResourceConfig c = new ResourceConfig();
        c.register(RedirectingResource.class);
        return c;
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
     * Load the test data.
     *
     * @return The test data.
     */
    @Override
    public File testData() {
        return null;
    }

    /**
     * Test that convenience methods are accessible.
     */
    @Test
    public void testTest() {
        Session s = getSession();
        SessionFactory f = getSessionFactory();

        Assert.assertTrue(s.isOpen());
        Assert.assertFalse(f.isClosed());

        Response r = target("/redirect")
                .queryParam("foo", "bar")
                .request()
                .get();
        Response r2 = followRedirect(r);

        String body = r2.readEntity(String.class);
        Assert.assertEquals("bar", body);
    }

    /**
     * A redirecting resource, for our test application.
     */
    @Path("/")
    public static final class RedirectingResource {

        /**
         * Redirect a request.
         *
         * @param foo Test passthrough value.
         * @return A redirection response.
         */
        @Path("/redirect")
        @GET
        public Response redirect(@QueryParam("foo") final String foo) {

            URI redirect = UriBuilder.fromPath("/redirected")
                    .queryParam("foo", foo)
                    .build();

            return Response.status(HttpStatus.SC_MOVED_TEMPORARILY)
                    .location(redirect)
                    .build();
        }

        /**
         * A redirected handler
         *
         * @param foo Test passthrough value.
         * @return An OK response.
         */
        @Path("/redirected")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response redirected(@QueryParam("foo") final String foo) {
            return Response.ok(foo).build();
        }
    }
}
