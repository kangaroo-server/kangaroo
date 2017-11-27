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
import net.krotscheck.kangaroo.test.NetworkUtil;
import net.krotscheck.kangaroo.test.rule.WorkingDirectoryRule;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the server factory.
 *
 * @author Michael Krotscheck
 */
public class ServerFactoryTest {

    /**
     * Ensure that we have a working directory.
     */
    @Rule
    public final WorkingDirectoryRule workingDirectory =
            new WorkingDirectoryRule();

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
     * Test building with a provided, invalid SSL certificate.
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
        String openPort = String.valueOf(NetworkUtil.findFreePort());

        File tempDir = workingDirectory.getWorkingDir();

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "-p=" + openPort,
                        "-h=localhost",
                        "--kangaroo.working_dir=" + tempDir.toString()
                });

        HttpServer s = f.build();
        s.start();

        File keystore = new File(tempDir, "generated.p12");
        assertTrue(keystore.exists());

        s.shutdownNow();
    }

    /**
     * Assert that the server generates a keystore.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testBuildGeneratesKeystore() throws Exception {
        URL filePath = this.getClass()
                .getResource("/config/test.properties");
        String openPort = String.valueOf(NetworkUtil.findFreePort());

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "-p=" + openPort,
                        "-h=localhost"
                })
                .withPropertiesFile(filePath.toString());

        HttpServer s = f.build();
        s.start();

        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet = new HttpGet("https://localhost:" + openPort + "/");
        CloseableHttpResponse response = httpclient.execute(httpGet);

        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                404);
        response.close();

        httpclient.close();
        s.shutdownNow();
    }

    /**
     * Assert that we can mount multiple applications at different url paths,
     * even if a root html app resource is declared.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testMountMultipleServlets() throws Exception {
        Path appRoot = Paths.get("src/test/resources/html/index");
        String appPath = appRoot.toAbsolutePath().toString();
        String openPort = String.valueOf(NetworkUtil.findFreePort());

        ResourceConfig one = new ResourceConfig();
        one.register(StatusFeature.class);

        ResourceConfig two = new ResourceConfig();
        two.register(StatusFeature.class);

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "--kangaroo.html_app_root=" + appPath,
                        "--kangaroo.port=" + openPort
                })
                .withResource("/one", one)
                .withResource("/two", two);

        HttpServer s = f.build();
        s.start();

        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet1 = new HttpGet(
                "https://localhost:" + openPort + "/one/status");
        CloseableHttpResponse response1 = httpclient.execute(httpGet1);
        Assert.assertEquals(response1.getStatusLine().getStatusCode(),
                200);
        response1.close();

        HttpGet httpGet2 = new HttpGet(
                "https://localhost:" + openPort + "/two/status");
        CloseableHttpResponse response2 = httpclient.execute(httpGet2);
        Assert.assertEquals(response2.getStatusLine().getStatusCode(),
                200);
        response2.close();

        HttpGet httpGet3 = new HttpGet("https://localhost:" + openPort + "/");
        CloseableHttpResponse response3 = httpclient.execute(httpGet3);
        String responseBody3 = EntityUtils.toString(response3.getEntity());

        Assert.assertEquals(response3.getStatusLine().getStatusCode(),
                200);
        Assert.assertEquals("Hello world", responseBody3);
        response3.close();

        httpclient.close();
        s.shutdownNow();
    }

    /**
     * Test building with an HTML5 application.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testAddHtmlApp() throws Exception {
        Path appRoot = Paths.get("src/test/resources/html/index");
        String appPath = appRoot.toAbsolutePath().toString();
        String openPort = String.valueOf(NetworkUtil.findFreePort());

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "--kangaroo.html_app_root=" + appPath,
                        "--kangaroo.port=" + openPort
                });

        HttpServer s = f.build();
        s.start();

        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet1 = new HttpGet("https://localhost:" + openPort + "/");
        CloseableHttpResponse response1 = httpclient.execute(httpGet1);
        String responseBody1 = EntityUtils.toString(response1.getEntity());

        Assert.assertEquals(response1.getStatusLine().getStatusCode(),
                200);
        Assert.assertEquals("Hello world", responseBody1);
        response1.close();

        httpclient.close();
        s.shutdownNow();
    }

    /**
     * Test that we can invoke configuration steps on the server.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testConfigureServer() throws Exception {
        ServerFactory f = new ServerFactory()
                .configureServer(s -> {
                    s.getServerConfiguration().setSessionTimeoutSeconds(1000);
                });

        HttpServer s = f.build();

        Assert.assertEquals(1000,
                s.getServerConfiguration().getSessionTimeoutSeconds());
    }

    /**
     * Ensure that grizzly doesn't expose itself.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testNoGrizzlyServer() throws Exception {
        String openPort = String.valueOf(NetworkUtil.findFreePort());
        ResourceConfig one = new ResourceConfig();
        one.register(StatusFeature.class);

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "--kangaroo.port=" + openPort
                })
                .withResource("/one", one);

        HttpServer s = f.build();
        s.start();

        CloseableHttpClient httpclient = getHttpClient();
        HttpGet httpGet1 = new HttpGet(
                "https://localhost:" + openPort + "/one/status");
        CloseableHttpResponse response1 = httpclient.execute(httpGet1);
        Assert.assertEquals(response1.getStatusLine().getStatusCode(),
                200);
        Assert.assertFalse(response1.containsHeader("Server"));
        response1.close();

        httpclient.close();
        s.shutdownNow();
    }

    /**
     * Test building with an HTML app that doesn't have an index file.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = RuntimeException.class)
    public void testAddHtmlAppWithoutIndex() throws Exception {
        Path appRoot = Paths.get("src/test/resources/html/indnoindexex");
        String appPath = appRoot.toAbsolutePath().toString();

        ServerFactory f = new ServerFactory()
                .withCommandlineArgs(new String[]{
                        "--kangaroo.html_app_root=" + appPath
                });

        f.build();
    }
}
