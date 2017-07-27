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

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.test.jerseyTest.ContainerTest;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.internal.ServiceLocatorImpl;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.IsCollectionContaining.hasItems;

/**
 * Unit tests for the CORS filter.
 *
 * @author Michael Krotscheck
 */
public class CORSFilterTest extends ContainerTest {

    /**
     * Create an application.
     *
     * @return The application to test.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(CORSFilter.class);
        config.register(new AllowedHeaders(new String[]{
                "One", "Two", "Three", ""
        }));
        config.register(new ExposedHeaders(new String[]{
                "One", "Two", "Three", ""
        }));
        // Inject a set of basic HTTP methods.
        config.register(new AllowedMethods(new String[]{
                HttpMethod.GET,
                HttpMethod.OPTIONS,
                ""
        }));

        config.register(new TestCORSValidator.Binder());
        config.register(MockService.class);
        return config;
    }

    /**
     * Assert that we can inject values using this binder.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testBinder() throws Exception {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocatorImpl locator = (ServiceLocatorImpl)
                factory.create(this.getClass().getSimpleName());

        ServiceLocatorUtilities.bind(locator, new CORSFilter.Binder());

        // Ensure it's a response filter.
        List<ActiveDescriptor<?>> respDescriptors =
                locator.getDescriptors(
                        BuilderHelper.createContractFilter(
                                ContainerResponseFilter.class.getName()));
        Assert.assertEquals(1, respDescriptors.size());

        ActiveDescriptor respDescriptor = respDescriptors.get(0);
        Assert.assertNotNull(respDescriptor);
        Assert.assertEquals(Singleton.class.getCanonicalName(),
                respDescriptor.getScope());
    }

    /**
     * Validate that the full set of expected headers (and values) are in the
     * received headers.
     *
     * @param expected List of expected headers & values.
     * @param received List of received headers.
     */
    private void validateContainsHeaders(
            final MultivaluedMap<String, Object> expected,
            final MultivaluedMap<String, Object> received) {

        expected.forEach((key, values) -> {
            Assert.assertTrue(received.containsKey(key));
            Assert.assertThat(received.get(key), hasItems(values.toArray()));
        });
    }

    /**
     * If the Origin header is not present terminate this set of steps. The
     * request is outside the scope of this specification.
     */
    @Test
    public void testRegularWithNoOrigin() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.add("One", "One");
        reqHeaders.add("Two", "Two");
        reqHeaders.add("Three", "Three");

        Response r = this.target("/")
                .request()
                .headers(reqHeaders)
                .build("GET")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * Validate the omitted headers.
     *
     * @param omittedHeaders The headers we do not want to see in a response.
     * @param received       The headers received.
     */
    private void validateOmitHeaders(
            final List<String> omittedHeaders,
            final MultivaluedMap<String, Object> received) {
        long foundHeaders = received.keySet().stream()
                .filter(omittedHeaders::contains)
                .count();
        Assert.assertEquals(0, foundHeaders);
    }

    /**
     * If the value of the Origin header is not a case-sensitive match for any
     * of the values in list of origins, do not set any additional headers and
     * terminate this set of steps.
     */
    @Test
    public void testRegularWithUnregisteredOrigin() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.add(HttpHeaders.ORIGIN, "http://invalid.example.com");
        reqHeaders.add("One", "One");
        reqHeaders.add("Two", "Two");
        reqHeaders.add("Three", "Three");

        Response r = this.target("/")
                .request()
                .headers(reqHeaders)
                .build("GET")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * If the HTTP method is not one of the permitted methods, do not set any
     * additional headers and terminate this set of steps.
     */
    @Test
    public void testRegularWithInvalidMethod() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.add(HttpHeaders.ORIGIN, "http://valid.example.com");
        reqHeaders.add("One", "One");
        reqHeaders.add("Two", "Two");
        reqHeaders.add("Three", "Three");

        Response r = this.target("/")
                .request()
                .headers(reqHeaders)
                .build("POST") // Not registered in the test app.
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * If the list of exposed headers is not empty add one or more
     * Access-Control-Expose-Headers headers, with as values the header field
     * names given in the list of exposed headers.
     */
    @Test
    public void testRegularWithValidOrigin() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.add(HttpHeaders.ORIGIN, "http://valid.example.com");
        reqHeaders.add("One", "One");
        reqHeaders.add("Two", "Two");
        reqHeaders.add("Three", "Three");

        Response r = this.target("/")
                .request()
                .headers(reqHeaders)
                .build("GET")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "one");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "two");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "three");

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * If the list of exposed headers is not empty add one or more
     * Access-Control-Expose-Headers headers, with as values the header field
     * names given in the list of exposed headers.
     */
    @Test
    public void testRegularWithValidOriginAndUnregisteredHeaders() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.add(HttpHeaders.ORIGIN, "http://valid.example.com");
        reqHeaders.add("One", "One");
        reqHeaders.add("Three", "Three");
        reqHeaders.add("Four", "Four");
        reqHeaders.add("Five", "Five");

        Response r = this.target("/")
                .request()
                .headers(reqHeaders)
                .build("GET")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "one");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "three");

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * If the Origin header is not present terminate this set of steps. The
     * request is outside the scope of this specification.
     */
    @Test
    public void testPreflightWithNoOrigin() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                "GET");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "One", "Two", "Three");

        Response r = this.target("/any")
                .request()
                .headers(reqHeaders)
                .build("OPTIONS")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(404, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * If the Origin header is not present terminate this set of steps. The
     * request is outside the scope of this specification.
     */
    @Test
    public void testPreflightWithInvalidOrigin() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.addAll(HttpHeaders.ORIGIN,
                "http://invalid.example.com");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                "GET");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "One", "Two", "Three");

        Response r = this.target("/any")
                .request()
                .headers(reqHeaders)
                .build("OPTIONS")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(404, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * If the Origin header is not present terminate this set of steps. The
     * request is outside the scope of this specification.
     */
    @Test
    public void testPreflightWithNoMethod() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.addAll(HttpHeaders.ORIGIN,
                "http://valid.example.com");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "One", "Two", "Three");

        Response r = this.target("/any")
                .request()
                .headers(reqHeaders)
                .build("OPTIONS")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * If the Origin header is not present terminate this set of steps. The
     * request is outside the scope of this specification.
     */
    @Test
    public void testPreflightWithInvalidMethod() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.addAll(HttpHeaders.ORIGIN,
                "http://valid.example.com");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                "POST");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "One", "Two", "Three");

        Response r = this.target("/any")
                .request()
                .headers(reqHeaders)
                .build("OPTIONS")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                HttpHeaders.ACCESS_CONTROL_MAX_AGE));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * If the requested headers are not in the list of exposed headers, do
     * not return them.
     */
    @Test
    public void testPreflightWithInvalidHeaders() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.addAll(HttpHeaders.ORIGIN,
                "http://valid.example.com");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                "GET");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "Four", "Five", "Six");

        Response r = this.target("/any")
                .request()
                .headers(reqHeaders)
                .build("OPTIONS")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://valid.example.com");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "300");

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * Test a valid preflight request.
     */
    @Test
    public void testValidPreflight() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.addAll(HttpHeaders.ORIGIN,
                "http://valid.example.com");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                "GET");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "One", "Two", "Three");

        Response r = this.target("/any")
                .request()
                .headers(reqHeaders)
                .build("OPTIONS")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://valid.example.com");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "one");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "two");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "three");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "300");

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * Test a valid preflight request against a resource that has an options
     * handler.
     */
    @Test
    public void testValidPreflightExistingResource() {
        MultivaluedMap<String, Object> reqHeaders = new MultivaluedHashMap<>();
        reqHeaders.addAll(HttpHeaders.ORIGIN,
                "http://valid.example.com");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                "GET");
        reqHeaders.addAll(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "One", "Two", "Three");

        Response r = this.target("/")
                .request()
                .headers(reqHeaders)
                .build("OPTIONS")
                .invoke();

        MultivaluedMap<String, Object> expHeaders = new MultivaluedHashMap<>();
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        expHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://valid.example.com");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "one");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "two");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "three");
        expHeaders.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "300");
        expHeaders.add("Test", "One");

        List<String> omittedHeaders = new ArrayList<>();
        omittedHeaders.addAll(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));

        Assert.assertEquals(200, r.getStatus());
        validateContainsHeaders(expHeaders, r.getHeaders());
        validateOmitHeaders(omittedHeaders, r.getHeaders());
    }

    /**
     * A simple endpoint that returns the system status.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * Always returns.
         *
         * @return HTTP Response object with some test headers.
         */
        @OPTIONS
        public Response handleOptions() {
            return Response
                    .status(Status.OK)
                    .header("Test", "One")
                    .build();
        }

        /**
         * Always returns.
         *
         * @return HTTP Response object with some test headers.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response handleGet() {
            return Response
                    .status(Status.OK)
                    .header("One", "One")
                    .header("Two", "Two")
                    .header("Three", "Three")
                    .header("Four", "Four")
                    .build();
        }

        /**
         * Always returns.
         *
         * @return HTTP Response object with some test headers.
         */
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        public Response handlePost() {
            return Response
                    .status(Status.OK)
                    .header("One", "One")
                    .header("Two", "Two")
                    .header("Three", "Three")
                    .header("Four", "Four")
                    .build();
        }
    }

}
