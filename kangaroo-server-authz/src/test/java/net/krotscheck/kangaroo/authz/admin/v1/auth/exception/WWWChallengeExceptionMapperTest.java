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

package net.krotscheck.kangaroo.authz.admin.v1.auth.exception;

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.authz.admin.v1.auth.OAuth2AuthFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Unit tests for exceptions that carry an authorization challenge.
 *
 * @author Michael Krotscheck
 */
public final class WWWChallengeExceptionMapperTest extends ContainerTest {

    /**
     * Create an application.
     *
     * @return The application to test.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(ErrorService.class);

        config.register(JacksonFeature.class);
        config.register(ExceptionFeature.class);
        config.register(OAuth2AuthFeature.class);
        return config;
    }

    /**
     * Assert that we get the correct headers back with a 401.
     */
    @Test
    public void test401Serialization() {
        Response r = target("/error/401").request().get();

        Assert.assertEquals(401, r.getStatus());

        Map<String, String> headerValues =
                decodeHeader(r.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));

        Assert.assertEquals(
                "unauthorized",
                headerValues.get("error"));
        Assert.assertEquals(
                "You are not authorized.",
                headerValues.get("error_description"));
        Assert.assertEquals(
                "one two",
                headerValues.get("scope"));

        URI realm = UriBuilder.fromUri(headerValues.get("realm")).build();
        Assert.assertEquals("/error", realm.getPath());
    }

    /**
     * Assert that we get the correct headers back with a 403.
     */
    @Test
    public void test403Serialization() {
        Response r = target("/error/403").request().get();

        Assert.assertEquals(403, r.getStatus());

        Map<String, String> headerValues =
                decodeHeader(r.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));

        Assert.assertEquals(
                "forbidden",
                headerValues.get("error"));
        Assert.assertEquals(
                "This token may not access this resource.",
                headerValues.get("error_description"));
        Assert.assertEquals(
                "one two",
                headerValues.get("scope"));

        URI realm = UriBuilder.fromUri(headerValues.get("realm")).build();
        Assert.assertEquals("/error", realm.getPath());
    }

    /**
     * Decode headers.
     *
     * @param headerString The string value of the header.
     * @return Map of key/value pairs.
     */
    private Map<String, String> decodeHeader(final String headerString) {

        Assert.assertTrue(headerString.indexOf("Bearer") == 0);

        Map<String, String> headerValues = new HashMap<>();
        Arrays.stream(headerString.substring(7).split(","))
                .map(String::trim)
                .map(s -> s.split("="))
                .peek(s -> s[1] = s[1].substring(1, s[1].length() - 1))
                .forEach((s) -> headerValues.put(s[0], s[1]));

        return headerValues;
    }

    /**
     * A simple error throwing endpoint.
     *
     * @author Michael Krotscheck
     */
    @Path("/error")
    public static final class ErrorService {

        /**
         * Return 401.
         *
         * @param uriInfo The injected URI info.
         * @return Nothing, error thrown.
         */
        @GET()
        @Path("/401")
        @Produces(MediaType.APPLICATION_JSON)
        public Response notAuthorized(@Context final UriInfo uriInfo) {
            throw new OAuth2NotAuthorizedException(uriInfo,
                    new String[]{"one", "two"});
        }

        /**
         * Return 403.
         *
         * @param uriInfo The injected URI info.
         * @return Nothing, error thrown.
         */
        @GET()
        @Path("/403")
        @Produces(MediaType.APPLICATION_JSON)
        public Response forbidden(@Context final UriInfo uriInfo) {
            throw new OAuth2ForbiddenException(uriInfo,
                    new String[]{"one", "two"});
        }
    }
}
