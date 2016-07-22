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

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.UUID;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Very basic tests for the HTTP Util.
 *
 * @author Michael Krotscheck
 */
public final class HttpUtilTest {

    /**
     * Decode a header
     *
     * @param header The login.
     * @return The decoded header parts, assuming a basic auth header.
     */
    private String[] decodeHeader(final String header) {
        String[] results = header.split("Basic ");
        byte[] decoded = Base64.decodeBase64(results[1]);
        try {
            String result = new String(decoded, "UTF-8");
            return result.split(":");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new String[]{};
    }

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = HttpUtil.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Test the bearer auth header.
     */
    @Test
    public void testAuthHeaderBearer() {
        UUID token = UUID.randomUUID();
        String expected = String.format("Bearer %s", token.toString());

        // Test via string.
        String header = HttpUtil.authHeaderBearer(token.toString());
        Assert.assertEquals(expected, header);

        // Test via uuid
        String uuidHeader = HttpUtil.authHeaderBearer(token);
        Assert.assertEquals(expected, uuidHeader);
    }

    /**
     * Test the basic auth header with strings.
     */
    @Test
    public void testAuthHeaderBasic() {
        String login = "login";
        String password = "password";

        String header = HttpUtil.authHeaderBasic(login, password);
        String[] decoded = decodeHeader(header);

        Assert.assertEquals(login, decoded[0]);
        Assert.assertEquals(password, decoded[1]);
    }

    /**
     * Test basic auth header with UUID's.
     */
    @Test
    public void testAuthHeaderBasicUUID() {
        UUID login = UUID.randomUUID();
        String password = "password";

        String header = HttpUtil.authHeaderBasic(login, password);
        String[] decoded = decodeHeader(header);

        Assert.assertEquals(login.toString(), decoded[0]);
        Assert.assertEquals(password, decoded[1]);
    }

    /**
     * Test decoding a query string from a URI.
     */
    @Test
    public void testParseQueryParamsUri() {
        URI uri = UriBuilder.fromUri("/foo?foo=bar&lol=cat&lol=doge").build();

        MultivaluedMap<String, String> results = HttpUtil.parseQueryParams(uri);
        Assert.assertTrue(results.containsKey("foo"));
        Assert.assertTrue(results.containsKey("lol"));
        Assert.assertTrue(results.get("foo").contains("bar"));
        Assert.assertTrue(results.get("lol").contains("cat"));
        Assert.assertTrue(results.get("lol").contains("doge"));
    }

    /**
     * Test decoding a query string from a query string.
     */
    @Test
    public void testParseQueryParamsString() {
        String uri = "foo=bar&lol=cat&lol=doge";

        MultivaluedMap<String, String> results = HttpUtil.parseQueryParams(uri);
        Assert.assertTrue(results.containsKey("foo"));
        Assert.assertTrue(results.containsKey("lol"));
        Assert.assertTrue(results.get("foo").contains("bar"));
        Assert.assertTrue(results.get("lol").contains("cat"));
        Assert.assertTrue(results.get("lol").contains("doge"));
    }

    /**
     * Test decoding a query string from a form-encoded body.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testParseBodyParamsValidStream() throws Exception {
        String body = "foo=bar&lol=cat&lol=doge";
        InputStream is = new ByteArrayInputStream(body.getBytes("UTF-8"));
        Response r = Mockito.mock(Response.class);
        Mockito.doReturn(is).when(r).getEntity();

        MultivaluedMap<String, String> results = HttpUtil.parseBodyParams(r);
        Assert.assertTrue(results.containsKey("foo"));
        Assert.assertTrue(results.containsKey("lol"));
        Assert.assertTrue(results.get("foo").contains("bar"));
        Assert.assertTrue(results.get("lol").contains("cat"));
        Assert.assertTrue(results.get("lol").contains("doge"));
    }

    /**
     * Test decoding a query string from a form-encoded body.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testParseBodyParamsInvalidStream() throws Exception {
        Response r = Mockito.mock(Response.class);
        Mockito.doThrow(IOException.class).when(r).getEntity();

        MultivaluedMap<String, String> results = HttpUtil.parseBodyParams(r);
        Assert.assertEquals(0, results.size());
    }
}
