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

package net.krotscheck.kangaroo.util;

import com.google.common.net.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.List;

/**
 * Unit tests for the HTTP Header utility.
 *
 * @author Michael Krotscheck
 */
public final class RequestUtilTest {

    /**
     * Build a test request context, with all necessary bits mocked.
     *
     * @param requestUri URL of the request to mock.
     * @param headers    The headers to add.
     * @return A request context.
     * @throws Exception Thrown if the passed URI doesn't validate.
     */
    public ContainerRequestContext buildContext(
            final String requestUri,
            final MultivaluedMap<String, String> headers)
            throws Exception {
        ContainerRequestContext mockContext =
                Mockito.mock(ContainerRequestContext.class);
        UriInfo mockInfo = Mockito.mock(UriInfo.class);
        URI uri = new URI(requestUri);

        Mockito.doReturn(uri).when(mockInfo).getRequestUri();
        Mockito.doReturn(headers).when(mockContext).getHeaders();
        Mockito.doReturn(mockInfo).when(mockContext).getUriInfo();

        return mockContext;
    }

    /**
     * Assert the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = RequestUtil.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Test CORS get method.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCorsRequestedMethod() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "OPTIONS");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );
        String method = RequestUtil.getCORSRequestedMethod(mockContext);
        Assert.assertEquals("OPTIONS", method);
    }

    /**
     * Test Empty CORS get method.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCorsRequestedMethodEmpty() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );
        String method = RequestUtil.getCORSRequestedMethod(mockContext);
        Assert.assertNull(method);
    }

    /**
     * Test CORS get headers.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCorsRequestedHeaders() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "One, TWO , "
                + "Three, Four,,Five");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );
        List<String> reqHeaders =
                RequestUtil.getCORSRequestedHeaders(mockContext);

        Assert.assertEquals(5, reqHeaders.size());
        Assert.assertTrue(reqHeaders.contains("one"));
        Assert.assertTrue(reqHeaders.contains("two"));
        Assert.assertTrue(reqHeaders.contains("three"));
        Assert.assertTrue(reqHeaders.contains("four"));
        Assert.assertTrue(reqHeaders.contains("five"));
    }

    /**
     * Test CORS get empty headers.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCorsRequestedEmptyHeaders() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );
        List<String> reqHeaders =
                RequestUtil.getCORSRequestedHeaders(mockContext);
        Assert.assertEquals(0, reqHeaders.size());
    }

    /**
     * Assert that we can get the host.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetHost() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.HOST, "host.example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );
        URI result = RequestUtil.getHost(mockContext);

        Assert.assertEquals("https://host.example.com",
                result.toString());
    }

    /**
     * Assert that we can get the host with a port.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetHostPort() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.HOST, "host.example.com:8080");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com:8080",
                headers
        );
        URI result = RequestUtil.getHost(mockContext);

        Assert.assertEquals("https://host.example.com:8080",
                result.toString());
    }

    /**
     * Assert that an invalid host header throws an exception.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = BadRequestException.class)
    public void testGetHostInvalid() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.HOST, "host.example.com:string");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );
        RequestUtil.getHost(mockContext);
    }

    /**
     * Assert that an invalid host header throws an exception.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = BadRequestException.class)
    public void testGetHostEmpty() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.HOST, "");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );
        RequestUtil.getHost(mockContext);
    }

    /**
     * Assert forwarded host works with expected values and types.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testForwardedHost() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "https");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "8080");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );
        URI result = RequestUtil.getForwardedHost(mockContext);

        Assert.assertEquals("https://example.com:8080",
                result.toString());
    }

    /**
     * Assert forwarded host works with no port.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testForwardedHostNoPort() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "https");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        URI result = RequestUtil.getForwardedHost(mockContext);

        Assert.assertEquals("https://example.com",
                result.toString());
    }

    /**
     * Assert forwarded host fails with no host.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testForwardedHostNoHost() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "https");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "8080");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        URI result = RequestUtil.getForwardedHost(mockContext);

        Assert.assertNull(result);
    }

    /**
     * Assert forwarded host fails with invalid host.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testForwardedHostInvalidHost() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "https");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "8080");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "     ");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        URI result = RequestUtil.getForwardedHost(mockContext);

        Assert.assertNull(result);
    }

    /**
     * Assert forwarded host fails with invalid host.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testForwardedHostInvalidPort() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "https");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "string");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        URI result = RequestUtil.getForwardedHost(mockContext);

        Assert.assertNull(result);
    }

    /**
     * Assert forwarded host fails with invalid protocol.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testForwardedHostNoProtocol() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.X_FORWARDED_PORT, "string");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        URI result = RequestUtil.getForwardedHost(mockContext);

        Assert.assertNull(result);
    }

    /**
     * Assert forwarded host fails with invalid protocol.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testForwardedHostInvalidProtocol() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "&&$$");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "string");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        URI result = RequestUtil.getForwardedHost(mockContext);

        Assert.assertNull(result);
    }

    /**
     * Test cross-origin with Host: only.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithHost() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.HOST, "host.example.com");
        ContainerRequestContext mockContext = buildContext(
                "http://host.example.com",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertFalse(result);
    }

    /**
     * Test cross-origin with Origin: and Host:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithOriginAndHost() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.ORIGIN, "http://host.example.com");
        headers.add(HttpHeaders.HOST, "host.example.com");
        ContainerRequestContext mockContext = buildContext(
                "http://host.example.com",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertFalse(result);
    }

    /**
     * Test cross-origin mismatch with Origin: and Host:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithOriginAndHostMismatch() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.ORIGIN, "http://example.com");
        headers.add(HttpHeaders.HOST, "host.example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertTrue(result);
    }

    /**
     * Test cross-origin with Origin: and X-Forwarded-*:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithOriginAndForward() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.ORIGIN, "http://example.com:8080");
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "http");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "8080");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        headers.add(HttpHeaders.HOST, "host.example.com");
        ContainerRequestContext mockContext = buildContext(
                "http://host.example.com",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertFalse(result);
    }

    /**
     * Test cross-origin mismatch with Origin: and X-Forwarded-*:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithOriginAndForwardMismatch() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.ORIGIN, "http://host.example.com:8080");
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "http");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "8081");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        headers.add(HttpHeaders.HOST, "host.example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com:8080",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertTrue(result);
    }

    /**
     * Test cross-origin with Origin: and invalid X-Forwarded-*:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = BadRequestException.class)
    public void testCrossOriginWithOriginAndInvalidForward() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.ORIGIN, "http://example.com:8080");
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "http");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "string");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        RequestUtil.isCrossOriginRequest(mockContext);
    }

    /**
     * Test cross-origin with Referrer: and Host:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithReferrerAndHost() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.REFERER,
                "http://host.example.com/some/page.html");
        headers.add(HttpHeaders.HOST,
                "host.example.com");
        ContainerRequestContext mockContext = buildContext(
                "http://host.example.com",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertFalse(result);
    }

    /**
     * Test cross-origin mismatch with Referrer: and Host:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithReferrerAndHostMismatch() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.REFERER, "http://example.com/some/page.html");
        headers.add(HttpHeaders.HOST, "another.example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertTrue(result);
    }

    /**
     * Test cross-origin with Referrer: and X-Forwarded-*:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithReferrerAndForward() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.HOST, "proxied.example.com");
        headers.add(HttpHeaders.REFERER,
                "http://example.com:8080/some/page.html");
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "http");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "8080");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertFalse(result);
    }

    /**
     * Test cross-origin mismatch with Referrer: and X-Forwarded-*:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCrossOriginWithReferrerAndForwardMisMatch()
            throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.HOST, "proxy.example.com");
        headers.add(HttpHeaders.REFERER, "http://example.com/some/page.html");
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "http");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "another.example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        Boolean result = RequestUtil.isCrossOriginRequest(mockContext);
        Assert.assertTrue(result);
    }

    /**
     * Test cross-origin with Referrer: and invalid X-Forwarded-*:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = BadRequestException.class)
    public void testCrossOriginWithReferrerAndInvalidForward()
            throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.REFERER, "http://example.com/some/page.html");
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "http");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "string");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        RequestUtil.isCrossOriginRequest(mockContext);
    }

    /**
     * Test cross-origin with no Origin: or Referrer:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = BadRequestException.class)
    public void testCrossOriginNoOriginOrReferrer() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.X_FORWARDED_PROTO, "http");
        headers.add(HttpHeaders.X_FORWARDED_PORT, "string");
        headers.add(HttpHeaders.X_FORWARDED_HOST, "example.com");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        RequestUtil.isCrossOriginRequest(mockContext);
    }

    /**
     * Test cross-origin with no Host: or X-Forwarded-*:.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = BadRequestException.class)
    public void testCrossOriginNoHostOrForward() throws Exception {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.ORIGIN, "http://example.com:8080");
        ContainerRequestContext mockContext = buildContext(
                "https://host.example.com",
                headers
        );

        RequestUtil.isCrossOriginRequest(mockContext);
    }
}
