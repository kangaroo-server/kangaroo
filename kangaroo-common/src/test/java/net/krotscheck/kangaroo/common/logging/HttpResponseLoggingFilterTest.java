/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.common.logging;

import ch.qos.logback.classic.Level;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.test.LoggingRule;
import net.krotscheck.kangaroo.test.jersey.KangarooJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Assert that the logging feature test.
 *
 * @author Michael Krotscheck
 */
public class HttpResponseLoggingFilterTest extends KangarooJerseyTest {

    /**
     * The logger rule.
     */
    @Rule
    public final LoggingRule logs =
            new LoggingRule(HttpResponseLoggingFilter.class, Level.ALL);

    /**
     * Clean the current logs.
     */
    @Before
    public void clearLogs() {
        // Clear the logs
        logs.clear();
    }

    /**
     * Build an application.
     *
     * @return A configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(JacksonFeature.class);
        config.register(HttpResponseLoggingFilter.class);
        config.register(MockService.class);

        return config;
    }

    /**
     * Test 200 error codes.
     */
    @Test
    public void test200ErrorCodes() {
        target("/error/200")
                .request()
                .get();
        List<String> messages = logs.getMessages();
        assertEquals(1, messages.size());
        assertEquals("200 HTTP GET error/200",
                messages.get(0));
    }

    /**
     * Test 3xx redirection codes.
     */
    @Test
    public void test300RedirectionErrorCodes() {
        target("/error/300")
                .request()
                .get();
        List<String> messages = logs.getMessages();
        assertEquals(1, messages.size());
        assertEquals("303 HTTP GET error/300 -> https://example.com/",
                messages.get(0));
    }

    /**
     * Test 3xx redirection codes without any redirection header.
     */
    @Test
    public void test300ErrorCodeWithNoLocation() {
        target("/error/300-no-location")
                .request()
                .get();
        List<String> messages = logs.getMessages();
        assertEquals(1, messages.size());
        assertEquals("300 HTTP GET error/300-no-location"
                        + " -> No location header provided",
                messages.get(0));
    }

    /**
     * Test 400 error codes with valid response bodies.
     */
    @Test
    public void test400ErrorCodesWithBody() {
        target("/error/400")
                .request()
                .get();
        List<String> messages = logs.getMessages();
        assertEquals(1, messages.size());
        assertEquals("400 HTTP GET error/400: Bad Request",
                messages.get(0));
    }

    /**
     * Test 400 error codes with no error body.
     */
    @Test
    public void test400ErrorCodesNoBody() {
        target("/error/400-no-body")
                .request()
                .get();
        List<String> messages = logs.getMessages();
        assertEquals(1, messages.size());
        assertEquals("400 HTTP GET error/400-no-body: No"
                        + " error entity detected.",
                messages.get(0));
    }

    /**
     * Test 500 error codes.
     */
    @Test
    public void test500ErrorCodesWithBody() {
        target("/error/500")
                .request()
                .get();
        List<String> messages = logs.getMessages();
        assertEquals(1, messages.size());
        assertEquals("500 HTTP GET error/500: Internal Server Error",
                messages.get(0));
    }

    /**
     * Test 500 error codes.
     */
    @Test
    public void test500ErrorCodesNoBody() {
        target("/error/500-no-body")
                .request()
                .get();
        List<String> messages = logs.getMessages();
        assertEquals(1, messages.size());
        assertEquals("500 HTTP GET error/500-no-body: No error"
                        + " entity detected.",
                messages.get(0));
    }

    /**
     * Test entity, returning various different error codes. Test for logging.
     *
     * @author Michael Krotscheck
     */
    @Path("/error")
    public static final class MockService {

        /**
         * Return a 2xx response.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/200")
        @Produces(MediaType.APPLICATION_JSON)
        public Response return200() {
            return Response.ok().build();
        }

        /**
         * Return a 3xx response.
         *
         * @return Nothing, error thrown.
         * @throws Exception e Should not be thrown.
         */
        @GET
        @Path("/300")
        @Produces(MediaType.APPLICATION_JSON)
        public Response return300() throws Exception {
            return Response
                    .seeOther(new URI("https://example.com/"))
                    .build();
        }

        /**
         * Return a 3xx response with no location header.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/300-no-location")
        @Produces(MediaType.APPLICATION_JSON)
        public Response return300NoLocation() {
            return Response.status(300).build();
        }

        /**
         * Return a 4xx response.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/400")
        @Produces(MediaType.APPLICATION_JSON)
        public Response return400() {
            return ErrorResponseBuilder
                    .from(Status.BAD_REQUEST)
                    .build();
        }

        /**
         * Return a 4xx response with no body.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/400-no-body")
        @Produces(MediaType.APPLICATION_JSON)
        public Response return400NoBody() {
            return Response
                    .status(400)
                    .build();
        }

        /**
         * Return a 5xx response.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/500")
        @Produces(MediaType.APPLICATION_JSON)
        public Response return500() {
            return ErrorResponseBuilder
                    .from(Status.INTERNAL_SERVER_ERROR)
                    .build();
        }

        /**
         * Return a 5xx response with no body.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/500-no-body")
        @Produces(MediaType.APPLICATION_JSON)
        public Response return500NoBody() {
            return Response.status(500).build();
        }
    }
}
