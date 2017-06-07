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

import net.krotscheck.kangaroo.common.status.StatusFeature;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unit tests for the server factory.
 *
 * @author Michael Krotscheck
 */
public class ServerFactoryTest {

    /**
     * Test an http server.
     *
     * @param server The server, will be started.
     */
    private void testHttpRequest(final HttpServer server) {
        Assert.assertNotNull(server);

        try {
            server.start();

            server.shutdownNow();
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Construct an HTTP client.
     *
     * @return An HTTP client that accepts all certificates.
     * @throws Exception Should not be thrown.
     */
    private CloseableHttpClient getHttpClient() throws Exception {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf =
                new SSLConnectionSocketFactory(builder.build());
        return HttpClients.custom().setSSLSocketFactory(sslsf).build();
    }

    /**
     * Test a simple server build.
     */
    @Test
    public void testSimpleBuild() {
        ServerFactory f = new ServerFactory();
        HttpServer s = f.build();
        Assert.assertNotNull(s);
    }

    /**
     * Try to build with an invalid configured URL.
     */
    @Test(expected = RuntimeException.class)
    public void testBuildInvalidURL() {
        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "-h=!@#$%^&*()",
                        "-p=10000"
                });
        f.build();
    }

    /**
     * Test building with an existing SSL certificate.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testWithProvidedCert() throws Exception {
        Path relativeCertPath =
                Paths.get("src/test/resources/ssl/test_keystore.p12");
        String certPath = relativeCertPath.toAbsolutePath().toString();

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "--kangaroo.keystore_path=" + certPath,
                        "--kangaroo.keystore_password=kangaroo",
                        "--kangaroo.keystore_type=PKCS12",
                        "--kangaroo.cert_alias=kangaroo",
                        "--kangaroo.cert_key_password=kangaroo"
                });
        Assert.assertNotNull(f.build());
    }

    /**
     * Test building with a provided, invalid SSL certificate
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = RuntimeException.class)
    public void testWithInvalidProvidedCert() throws Exception {
        Path relativeCertPath =
                Paths.get("src/test/resources/ssl/test_keystore.p12");
        String certPath = relativeCertPath.toAbsolutePath().toString();

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "--kangaroo.keystore_path=" + certPath,
                        "--kangaroo.keystore_password=invalidPass",
                        "--kangaroo.keystore_type=PKCS12",
                        "--kangaroo.cert_alias=kangaroo",
                        "--kangaroo.cert_key_password=kangaroo"
                });
        f.build();
    }

    /**
     * Assert that we can provide configuration from all configuration sources.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testBuildAllConfigOptions() throws Exception {
        URL filePath = this.getClass()
                .getResource("/config/test.properties");

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{"-p=9000", "-h=localhost"})
                .withPropertiesFile(filePath.toString());

        HttpServer s = f.build();
        s.start();

        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet = new HttpGet("https://localhost:9000/");
        CloseableHttpResponse response = httpclient.execute(httpGet);

        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                404);

        httpclient.close();
    }

    /**
     * Assert that we can mount multiple applications at different url paths.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testMountMultipleServlets() throws Exception {
        ResourceConfig one = new ResourceConfig();
        one.register(StatusFeature.class);

        ResourceConfig two = new ResourceConfig();
        two.register(StatusFeature.class);

        ServerFactory f = new ServerFactory()
                .withResource("/one", one)
                .withResource("/two", two);

        HttpServer s = f.build();
        s.start();

        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet1 = new HttpGet("https://localhost:8080/one/status");
        CloseableHttpResponse response1 = httpclient.execute(httpGet1);
        Assert.assertEquals(response1.getStatusLine().getStatusCode(),
                200);

        HttpGet httpGet2 = new HttpGet("https://localhost:8080/two/status");
        CloseableHttpResponse response2 = httpclient.execute(httpGet2);
        Assert.assertEquals(response2.getStatusLine().getStatusCode(),
                200);

        httpclient.close();
    }
}
