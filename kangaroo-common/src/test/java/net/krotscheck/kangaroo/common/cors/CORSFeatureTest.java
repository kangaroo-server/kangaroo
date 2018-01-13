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

package net.krotscheck.kangaroo.common.cors;

import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Test the CORS feature, by injecting it and accessing its values.
 *
 * @author Michael Krotscheck
 */
public final class CORSFeatureTest extends ContainerTest {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<List<String>> LIST_TYPE =
            new GenericType<List<String>>() {

            };

    /**
     * Create an application.
     *
     * @return The application to test.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(new TestCORSValidator.Binder());
        a.register(CORSFeature.class);
        a.register(MockService.class);
        return a;
    }

    /**
     * Get the available allowed headers.
     */
    @Test
    public void testAllowedHeaders() {
        Response response = target("/allowed_headers").request().get();
        List<String> results = response.readEntity(LIST_TYPE);
        Assert.assertTrue(results.contains(HttpHeaders.ACCEPT));
        Assert.assertTrue(results.contains(HttpHeaders.ACCEPT_LANGUAGE));
        Assert.assertTrue(results.contains(HttpHeaders.CONTENT_LANGUAGE));
        Assert.assertTrue(results.contains(HttpHeaders.AUTHORIZATION));
        Assert.assertTrue(results.contains(HttpHeaders.CONTENT_TYPE));
        Assert.assertTrue(results.contains(HttpHeaders.ORIGIN));
        Assert.assertTrue(results.contains(HttpHeaders.X_REQUESTED_WITH));
    }

    /**
     * Get the available allowed methods.
     */
    @Test
    public void testAllowedMethods() {
        Response response = target("/allowed_methods").request().get();
        List<String> results = response.readEntity(LIST_TYPE);
        Assert.assertTrue(results.contains(HttpMethod.GET));
        Assert.assertTrue(results.contains(HttpMethod.PUT));
        Assert.assertTrue(results.contains(HttpMethod.POST));
        Assert.assertTrue(results.contains(HttpMethod.DELETE));
        Assert.assertTrue(results.contains(HttpMethod.OPTIONS));
    }

    /**
     * Get the available exposed headers.
     */
    @Test
    public void testExposedHeaders() {
        Response response = target("/exposed_headers").request().get();
        List<String> results = response.readEntity(LIST_TYPE);
        Assert.assertTrue(results.contains(HttpHeaders.LOCATION));
        Assert.assertTrue(results.contains(HttpHeaders.WWW_AUTHENTICATE));
        Assert.assertTrue(results.contains(HttpHeaders.CACHE_CONTROL));
        Assert.assertTrue(results.contains(HttpHeaders.CONTENT_LANGUAGE));
        Assert.assertTrue(results.contains(HttpHeaders.CONTENT_TYPE));
        Assert.assertTrue(results.contains(HttpHeaders.EXPIRES));
        Assert.assertTrue(results.contains(HttpHeaders.LAST_MODIFIED));
        Assert.assertTrue(results.contains(HttpHeaders.PRAGMA));
    }

    /**
     * Testing endpoint.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * List of allowed headers.
         */
        private List<String> allowedHeaders;

        /**
         * List of exposed headers.
         */
        private List<String> exposedHeaders;

        /**
         * List of allowed methods.
         */
        private List<String> allowedMethods;

        /**
         * Create a new instance of the test service.
         *
         * @param allowedHeaders Injected allowed headers.
         * @param exposedHeaders Injected exposed headers.
         * @param allowedMethods Injected allowed methods.
         */
        @Inject
        public MockService(@Named(AllowedHeaders.NAME) final
                           Iterable<String> allowedHeaders,
                           @Named(ExposedHeaders.NAME) final
                           Iterable<String> exposedHeaders,
                           @Named(AllowedMethods.NAME) final
                           Iterable<String> allowedMethods) {
            this.allowedHeaders = Lists.newArrayList(allowedHeaders);
            this.exposedHeaders = Lists.newArrayList(exposedHeaders);
            this.allowedMethods = Lists.newArrayList(allowedMethods);
        }

        /**
         * Return the allowed headers.
         *
         * @return HTTP Response object with the current service status.
         */
        @Path("/allowed_methods")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAllowedMethods() {
            return Response
                    .status(HttpStatus.SC_OK)
                    .entity(allowedMethods)
                    .build();
        }

        /**
         * Return the exposed headers.
         *
         * @return HTTP Response object with the current service status.
         */
        @Path("/exposed_headers")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getExposedHeaders() {
            return Response
                    .status(HttpStatus.SC_OK)
                    .entity(exposedHeaders)
                    .build();
        }

        /**
         * Return the allowed methods.
         *
         * @return HTTP Response object with the current service status.
         */
        @Path("/allowed_headers")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAllowedHeaders() {
            return Response
                    .status(HttpStatus.SC_OK)
                    .entity(allowedHeaders)
                    .build();
        }
    }
}
