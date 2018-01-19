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

package net.krotscheck.kangaroo.server;

import net.krotscheck.kangaroo.test.NetworkUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for our HTML Application HTTP handler.
 *
 * @author Michael Krotscheck
 */
public final class HtmlApplicationHttpHandlerTest {

    /**
     * The server under test, which contains our handler.
     */
    private static HttpServer server;

    /**
     * The port in use by the server.
     */
    private static String port;

    /**
     * Create and start up the server.
     *
     * @throws Exception Should not be thrown.
     */
    @BeforeClass
    public static void startServer() throws Exception {
        port = String.valueOf(NetworkUtil.findFreePort());
        URI serverUri = UriBuilder
                .fromUri("http://localhost:" + port + "/")
                .build();
        String appRoot = Paths.get("src/test/resources/html/index")
                .toAbsolutePath().toString();
        server = GrizzlyHttpServerFactory.createHttpServer(serverUri, false);
        HtmlApplicationHttpHandler handler =
                new HtmlApplicationHttpHandler(appRoot);
        server.getServerConfiguration().addHttpHandler(handler, "/*");
        server.start();
    }

    /**
     * Shut down the server.
     */
    @AfterClass
    public static void stopServer() {
        server.shutdownNow();
        server = null;
    }

    /**
     * Construct an HTTP client.
     *
     * @return An HTTP client that accepts all certificates.
     * @throws Exception Should not be thrown.
     */
    private CloseableHttpClient getHttpClient() throws Exception {
        return HttpClients.createDefault();
    }

    /**
     * Assert that we cannot create an instance with an invalid root directory.
     *
     * @throws Exception Should be thrown
     */
    @Test(expected = RuntimeException.class)
    public void handleCreateWithInvalidRoot() throws Exception {
        String appRoot = Paths.get("src/test/resources/html/noindex")
                .toAbsolutePath().toString();
        new HtmlApplicationHttpHandler(appRoot);
    }

    /**
     * Assert that we can read the index.html file.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void handleReadIndex() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet1 =
                new HttpGet("http://localhost:" + port + "/index.html");
        CloseableHttpResponse response = httpclient.execute(httpGet1);
        String responseBody1 = EntityUtils.toString(response.getEntity());
        response.close();

        assertEquals(response.getStatusLine().getStatusCode(),
                200);
        assertEquals("Hello world", responseBody1);

        httpclient.close();
    }

    /**
     * Assert that we can read the root directory and get index.html.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void handleReadRootPath() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet1 = new HttpGet("http://localhost:" + port);
        CloseableHttpResponse response = httpclient.execute(httpGet1);
        String responseBody1 = EntityUtils.toString(response.getEntity());
        response.close();

        assertEquals(response.getStatusLine().getStatusCode(),
                200);
        assertEquals("Hello world", responseBody1);

        httpclient.close();
    }

    /**
     * Assert that a valid subdirectory request returns index.html.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void handleValidSubdirectory() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet1 =
                new HttpGet("http://localhost:" + port + "/subdir/");
        CloseableHttpResponse response = httpclient.execute(httpGet1);
        String responseBody1 = EntityUtils.toString(response.getEntity());
        response.close();

        assertEquals(response.getStatusLine().getStatusCode(),
                200);
        assertEquals("Hello world", responseBody1);

        httpclient.close();
    }

    /**
     * Assert that a 404 request returns index.html.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void handle404Resource() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet1 =
                new HttpGet("http://localhost:" + port + "/invalid.html");
        CloseableHttpResponse response = httpclient.execute(httpGet1);
        String responseBody1 = EntityUtils.toString(response.getEntity());
        response.close();

        assertEquals(response.getStatusLine().getStatusCode(),
                200);
        assertEquals("Hello world", responseBody1);

        httpclient.close();
    }

    /**
     * Assert that an existing subresource is returned properly.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void handleValidSubresource() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet1 =
                new HttpGet("http://localhost:" + port + "/subdir/other.html");
        CloseableHttpResponse response = httpclient.execute(httpGet1);
        String responseBody1 = EntityUtils.toString(response.getEntity());
        response.close();

        assertEquals(response.getStatusLine().getStatusCode(),
                200);
        assertEquals("Other hello world", responseBody1);

        httpclient.close();
    }

    /**
     * Assert that a non-get method throws an appropriate response.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testNonGetResource() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpDelete httpDelete = new HttpDelete(
                "http://localhost:" + port + "/subdir/other.html");
        CloseableHttpResponse response = httpclient.execute(httpDelete);
        String responseBody1 = EntityUtils.toString(response.getEntity());
        response.close();

        assertEquals(response.getStatusLine().getStatusCode(),
                405);

        httpclient.close();
    }
}
