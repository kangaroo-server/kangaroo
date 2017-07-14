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

import com.google.common.base.Strings;
import net.krotscheck.kangaroo.common.config.ConfigurationBinder;
import net.krotscheck.kangaroo.server.keystore.FSKeystoreProvider;
import net.krotscheck.kangaroo.server.keystore.GeneratedKeystoreProvider;
import net.krotscheck.kangaroo.server.keystore.IKeystoreProvider;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.ServletRegistration;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A factory class that allows us to build Kangaroo-style grizzly servers.
 *
 * @author Michael Krotscheck
 */
public final class ServerFactory {

    /**
     * Configuration builder.
     */
    private ConfigurationBuilder configBuilder = new ConfigurationBuilder();

    /**
     * A mapping of Path:Hosted Resource Config.
     */
    private Map<String, ResourceConfig> services = new HashMap<>();

    /**
     * Create a new server factory.
     */
    public ServerFactory() {
    }

    /**
     * Add a set of commandline arguments to this server's configuration.
     * Note that these will be added in invoked priority.
     *
     * @param args The arguments to add.
     * @return This builder.
     */
    public ServerFactory withCommandlineArgs(final String[] args) {
        configBuilder.addCommandlineArgs(args);
        return this;
    }

    /**
     * Add a properties file to the server configuration. If we cannot find
     * the file, this will log a warning and continue.
     *
     * @param path Path to the properties file.
     * @return This factory.
     */
    public ServerFactory withPropertiesFile(final String path) {
        configBuilder.addPropertiesFile(path);
        return this;
    }

    /**
     * Add a resource to the server.
     *
     * @param s        The path under which to host the resource.
     * @param resource The resource configuration.
     * @return This factory.
     */
    public ServerFactory withResource(final String s,
                                      final ResourceConfig resource) {
        this.services.put(s, resource);
        return this;
    }

    /**
     * Build the server.
     *
     * @return A server, ready to run.
     */
    public HttpServer build() {
        Configuration config = configBuilder.build();
        HttpServer server = createServer(config);

        for (Entry<String, ResourceConfig> route : services.entrySet()) {
            String path = route.getKey();
            ResourceConfig rc = route.getValue();
            rc.register(new ConfigurationBinder(config));
            String name = route.getValue().getClass().getSimpleName();

            WebappContext context = new WebappContext(name, path);
            ServletRegistration registration =
                    context.addServlet(name, new ServletContainer(rc));
            registration.addMapping(String.format("%s/*", path));

            context.deploy(server);
        }

        // Build a static HTTP handler, serving from --kangaroo.html_app_root
        String htmlAppRoot = config.getString(Config.HTML_APP_ROOT.getKey(),
                Config.HTML_APP_ROOT.getValue());
        if (!Strings.isNullOrEmpty(htmlAppRoot)) {
            HtmlApplicationHttpHandler handler =
                    new HtmlApplicationHttpHandler(htmlAppRoot);
            handler.setFileCacheEnabled(true);
            server.getServerConfiguration().addHttpHandler(handler, "/*");
        }

        return server;
    }

    /**
     * Create a new HTTP server.
     *
     * @param config The server configuration.
     * @return An HTTP server, with no bound contexts.
     */
    private HttpServer createServer(final Configuration config) {
        URI serverUri = getServerUri(config);
        SSLEngineConfigurator configurator = buildSSLConfigurator(config);

        return GrizzlyHttpServerFactory.createHttpServer(serverUri,
                (GrizzlyHttpContainer) null,
                true,
                configurator,
                false);
    }

    /**
     * Attempt to load the configured keystore for this running instance, and
     * return it.
     *
     * @param ksPath    Path to the keystore. If not provided, a key will be
     *                  generated.
     * @param ksPass    Keystore Password.
     * @param ksType    Keystore Type.
     * @param certAlias Certificate alias.
     * @param certPass  Certificate key password.
     * @return A KeystoreProvider, unless an error occurs.
     */
    private IKeystoreProvider getKeystore(final String ksPath,
                                          final String ksPass,
                                          final String ksType,
                                          final String certAlias,
                                          final String certPass) {

        if (StringUtils.isEmpty(ksPath)) {
            return new GeneratedKeystoreProvider(ksPass, certPass, certAlias);
        }
        return new FSKeystoreProvider(ksPath, ksPass, ksType);
    }

    /**
     * Build the SSL configuration.
     *
     * @param config Server configuration.
     * @return An ssl engine configurator.
     */
    private SSLEngineConfigurator buildSSLConfigurator(
            final Configuration config) {
        String ksPath = config.getString(Config.KEYSTORE_PATH.getKey(),
                Config.KEYSTORE_PATH.getValue());
        String ksPass = config.getString(Config.KEYSTORE_PASS.getKey(),
                Config.KEYSTORE_PASS.getValue());
        String ksType = config.getString(Config.KEYSTORE_TYPE.getKey(),
                Config.KEYSTORE_TYPE.getValue());
        String certAlias = config.getString(Config.CERT_ALIAS.getKey(),
                Config.CERT_ALIAS.getValue());
        String certPass = config.getString(Config.CERT_KEY_PASS.getKey(),
                Config.CERT_KEY_PASS.getValue());

        // Build and store the keystore.
        IKeystoreProvider ksProvider = getKeystore(ksPath, ksPass, ksType,
                certAlias, certPass);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ksProvider.writeTo(baos);

        SSLContextConfigurator sslCon = new SSLContextConfigurator();
        sslCon.setKeyStoreBytes(baos.toByteArray());
        sslCon.setKeyPass(certPass);
        sslCon.setKeyStorePass(ksPass);

        SSLEngineConfigurator sslConf = new SSLEngineConfigurator(sslCon);
        sslConf.setClientMode(false);
        sslConf.setNeedClientAuth(false);
        return sslConf;
    }

    /**
     * Build the server URI from our internal configuration.
     *
     * @param config A configuration instance from which to read common
     *               properties.
     * @return The URI at which the server should be hosted.
     */
    private URI getServerUri(final Configuration config) {
        String host = config.getString(Config.HOST.getKey(),
                Config.HOST.getValue());
        Integer port = config.getInt(Config.PORT.getKey(),
                Config.PORT.getValue());
        String scheme = "http";

        URIBuilder builder = new URIBuilder();
        builder.setHost(host);
        builder.setPort(port);
        builder.setScheme(scheme);
        builder.setPath("/");

        try {
            return builder.build();
        } catch (URISyntaxException use) {
            throw new RuntimeException("Cannot construct server URI", use);
        }
    }
}
