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

package net.krotscheck.kangaroo.common.exception;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.exception.exception.HttpNotFoundException;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;
import net.krotscheck.kangaroo.test.KangarooJerseyTest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.mock;

/**
 * Test the exception mapping feature.
 *
 * @author Michael Krotscheck
 */
public final class ExceptionFeatureTest extends KangarooJerseyTest {

    /**
     * Build the application.
     *
     * @return An application that can parse exceptions.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(ExceptionFeature.class);
        a.register(MockService.class);
        a.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        a.register(JacksonJaxbJsonProvider.class);
        return a;
    }

    /**
     * Confirm that HTTP status parsing is loaded.
     */
    @Test
    public void testHttpStatusException() {
        Response r = target("/http").request().get();

        ErrorResponse response = r.readEntity(ErrorResponse.class);

        Assert.assertEquals(404, r.getStatus());
        Assert.assertEquals("not_found", response.getError());
        Assert.assertEquals("Not Found", response.getErrorDescription());
    }

    /**
     * Confirm that jersey exception parsing is loaded.
     */
    @Test
    public void testJerseyException() {
        Response r = target("/jersey").request().get();

        ErrorResponse response = r.readEntity(ErrorResponse.class);

        Assert.assertEquals(500, r.getStatus());
        Assert.assertEquals("internal_server_error", response.getError());
        Assert.assertEquals("Internal Server Error",
                response.getErrorDescription());
    }

    /**
     * Confirm that JSON exception parsing is loaded.
     */
    @Test
    public void testJsonException() {
        Response r = target("/json").request().get();

        ErrorResponse response = r.readEntity(ErrorResponse.class);

        Assert.assertEquals(400, r.getStatus());
        Assert.assertEquals("bad_request", response.getError());
    }

    /**
     * Confirm that generic exception parsing is loaded.
     */
    @Test
    public void testGenericException() {
        Response r = target("/generic").request().get();

        ErrorResponse response = r.readEntity(ErrorResponse.class);

        Assert.assertEquals(500, r.getStatus());
        Assert.assertEquals("internal_server_error", response.getError());
        Assert.assertEquals("Internal Server Error",
                response.getErrorDescription());
    }

    /**
     * A simple endpoint that returns the system status.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * Throw an HttpStatusException.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/http")
        @Produces(MediaType.APPLICATION_JSON)
        public Response http() {
            throw new HttpNotFoundException();
        }

        /**
         * Throw a JerseyException.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/jersey")
        @Produces(MediaType.APPLICATION_JSON)
        public Response jersey() {
            throw new WebApplicationException();
        }

        /**
         * Throw a JsonParseException.
         *
         * @return Nothing, error thrown.
         * @throws Exception Test Exception.
         */
        @GET
        @Path("/json")
        @Produces(MediaType.APPLICATION_JSON)
        public Response json() throws Exception {
            throw new JsonParseException("foo", mock(JsonLocation.class));
        }

        /**
         * Throw a generic exception.
         *
         * @return Nothing, error thrown.
         * @throws Exception Test Exception.
         */
        @GET
        @Path("/generic")
        @Produces(MediaType.APPLICATION_JSON)
        public Response generic() throws Exception {
            throw new Exception("foo");
        }
    }
}
