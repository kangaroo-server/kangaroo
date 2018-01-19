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

import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the HTTP Util.
 *
 * @author Michael Krotscheck
 */
public final class HttpUtilTest {

    /**
     * The character set we're using.
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");

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
     * Test that basic auth headers can be constructed.
     */
    @Test
    public void authHeaderBasic() {
        assertEquals("Basic bG9naW46cGFzc3dvcmQ=",
                HttpUtil.authHeaderBasic("login", "password"));
        assertEquals("", HttpUtil.authHeaderBasic((String) null, "password"));
        assertEquals("", HttpUtil.authHeaderBasic("login", null));
        assertEquals("Basic MDAwMDAwMDAwMDAwMDAwMDAwMDAw"
                        + "MDAwMDAwMDAwMGE6cGFzc3dvcmQ=",
                HttpUtil.authHeaderBasic(BigInteger.TEN, "password"));
    }

    /**
     * Assert that the bearer header can be constructed.
     */
    @Test
    public void authHeaderBearer() {
        assertEquals("Bearer test_token",
                HttpUtil.authHeaderBearer("test_token"));
        assertEquals("Bearer 0000000000000000000000000000000a",
                HttpUtil.authHeaderBearer(BigInteger.TEN));
        assertEquals("", HttpUtil.authHeaderBearer((String) null));
    }

    /**
     * Assert that we can parse params from a URI.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void parseQueryParamsFromUri() throws Exception {
        URI singleParams =
                new URIBuilder("http://example.com/?foo=bar&lol=cat")
                        .build();
        MultivaluedMap<String, String> params
                = HttpUtil.parseQueryParams(singleParams);
        assertEquals(2, params.size());
        assertEquals("cat", params.getFirst("lol"));
        assertEquals("bar", params.getFirst("foo"));

        MultivaluedMap<String, String> params2
                = HttpUtil.parseQueryParams((URI) null);
        assertEquals(0, params2.size());
    }

    /**
     * Assert that we can pull data from a request body.
     */
    @Test
    public void parseQueryParamsFromBody() {
        Response r = mock(Response.class);

        InputStream stream1 = IOUtils.toInputStream("foo=bar&lol=cat", UTF8);
        doReturn(stream1).when(r).getEntity();
        MultivaluedMap<String, String> params = HttpUtil.parseBodyParams(r);
        assertEquals(2, params.size());
        assertEquals("cat", params.getFirst("lol"));
        assertEquals("bar", params.getFirst("foo"));

        InputStream stream2 = IOUtils.toInputStream("", UTF8);
        doReturn(stream2).when(r).getEntity();
        MultivaluedMap<String, String> params2 = HttpUtil.parseBodyParams(r);
        assertEquals(0, params2.size());

        InputStream stream3 = IOUtils.toInputStream("", UTF8);
        doThrow(IOException.class).when(r).getEntity();
        MultivaluedMap<String, String> params3 = HttpUtil.parseBodyParams(r);
        assertEquals(0, params3.size());
    }

    /**
     * Assert that we can read various permutatinos of parameters from a raw
     * string.
     */
    @Test
    public void parseBodyParamsFromRawString() {
        MultivaluedMap<String, String> params
                = HttpUtil.parseQueryParams("foo=bar&lol=cat");
        assertEquals(2, params.size());
        assertEquals("cat", params.getFirst("lol"));
        assertEquals("bar", params.getFirst("foo"));

        MultivaluedMap<String, String> params2
                = HttpUtil.parseQueryParams("");
        assertEquals(0, params2.size());

        MultivaluedMap<String, String> params3
                = HttpUtil.parseQueryParams((String) null);
        assertEquals(0, params3.size());

        MultivaluedMap<String, String> params4
                = HttpUtil.parseQueryParams("noValue");
        assertEquals(1, params4.size());
        assertNull(params.getFirst("noValue"));

        MultivaluedMap<String, String> params5
                = HttpUtil.parseQueryParams("foo=bar&foo=baz");
        assertEquals(1, params5.size());
        assertEquals(2, params5.get("foo").size());
        assertTrue(params5.get("foo").contains("bar"));
        assertTrue(params5.get("foo").contains("baz"));
    }

}
