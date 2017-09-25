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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.krotscheck.kangaroo.common.config.ConfigurationBinder;
import net.krotscheck.kangaroo.server.keystore.FSKeystoreProvider;
import net.krotscheck.kangaroo.server.keystore.GeneratedKeystoreProvider;
import net.krotscheck.kangaroo.server.keystore.IKeystoreProvider;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.ServletRegistration;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A factory class that allows us to build Kangaroo-style grizzly servers.
 *
 * @author Michael Krotscheck
 */
public final class ServerFactory {

    /**
     * Our commandline options.
     */
    public static final Options CLI_OPTIONS;

    static {
        // Initialize the CLI Options.
        CLI_OPTIONS = new Options();

        Option bindHost = Option.builder("h")
                .longOpt(Config.HOST.getKey())
                .argName(Config.HOST.getKey())
                .hasArg()
                .desc("The IP address or hostname which this server should "
                        + "bind to.")
                .build();
        Option bindPort = Option.builder("p")
                .longOpt(Config.PORT.getKey())
                .argName(Config.PORT.getKey())
                .hasArg()
                .desc("The port on which this port should listen.")
                .build();

        Option keystorePath = Option.builder()
                .longOpt(Config.KEYSTORE_PATH.getKey())
                .argName(Config.KEYSTORE_PATH.getKey())
                .hasArg()
                .desc("Path to an externally provided keystore.")
                .build();

        Option keystorePass = Option.builder()
                .longOpt(Config.KEYSTORE_PASS.getKey())
                .argName(Config.KEYSTORE_PASS.getKey())
                .hasArg()
                .desc("Password for the externally provided keystore.")
                .build();

        Option keystoreType = Option.builder()
                .longOpt(Config.KEYSTORE_TYPE.getKey())
                .argName(Config.KEYSTORE_TYPE.getKey())
                .hasArg()
                .desc("JVM KeyStore type to expect. Default PKCS12.")
                .build();

        Option certAlias = Option.builder()
                .longOpt(Config.CERT_ALIAS.getKey())
                .argName(Config.CERT_ALIAS.getKey())
                .hasArg()
                .desc("Alias of the HTTPS certificate to use.")
                .build();

        Option certKeyPass = Option.builder()
                .longOpt(Config.CERT_KEY_PASS.getKey())
                .argName(Config.CERT_KEY_PASS.getKey())
                .hasArg()
                .desc("Password of the private key for the certificate.")
                .build();

        Option htmlAppRoot = Option.builder()
                .longOpt(Config.HTML_APP_ROOT.getKey())
                .argName(Config.HTML_APP_ROOT.getKey())
                .hasArg()
                .desc("Path to the server's HTML5 Application root. We "
                        + "presume HTML5 routing support in the application "
                        + "served there.")
                .build();

        CLI_OPTIONS.addOption(bindHost);
        CLI_OPTIONS.addOption(bindPort);
        CLI_OPTIONS.addOption(keystorePath);
        CLI_OPTIONS.addOption(keystorePass);
        CLI_OPTIONS.addOption(keystoreType);
        CLI_OPTIONS.addOption(certAlias);
        CLI_OPTIONS.addOption(certKeyPass);
        CLI_OPTIONS.addOption(htmlAppRoot);
    }

    /**
     * List of configuration operators to apply.
     */
    private final List<ServerOperator> serverLambdas =
            new ArrayList<>();

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
        // Add the server's commandline options.
        withCommandlineOptions(CLI_OPTIONS);

        // Certain values are required, so set those in the default
        // configuration.
        Map<String, Object> defaults = new HashMap<>();
        defaults.put(Config.HOST.getKey(), Config.HOST.getValue());
        defaults.put(Config.PORT.getKey(), Config.PORT.getValue());
        configBuilder.withDefaults(defaults);
    }

    /**
     * Add additional commandline options to the server.
     *
     * @param options The options to add.
     * @return This builder.
     */
    public ServerFactory withCommandlineOptions(final Options options) {
        configBuilder.withCommandlineOptions(options);
        return this;
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

        // Code below taken from the GrizzlyHttpServerFactory - if something
        // breaks during an upgrade, check there.
        final NetworkListener listener = new NetworkListener("kangaroo",
                serverUri.getHost(),
                serverUri.getPort());

        listener.getTransport()
                .getWorkerThreadPoolConfig()
                .setThreadFactory(new ThreadFactoryBuilder()
                        .setNameFormat("kangaroo-http-server-%d")
                        .setUncaughtExceptionHandler(
                                new JerseyProcessingUncaughtExceptionHandler())
                        .build());
        listener.setSecure(true);
        listener.setSSLEngineConfig(configurator);

        final HttpServer server = new HttpServer();
        server.addListener(listener);

        // Map the path to the processor.
        final ServerConfiguration serverConfiguration =
                server.getServerConfiguration();
        serverConfiguration.setPassTraceRequest(true);
        serverConfiguration.setDefaultQueryEncoding(Charsets.UTF8_CHARSET);
        serverLambdas.forEach(s -> s.operation(server));

        return server;
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

    /**
     * Add a lambda which can modify the server configuration before it is
     * passed back to the client.
     *
     * @param serverLambda The operator.
     * @return This factory.
     */
    public ServerFactory configureServer(final ServerOperator serverLambda) {
        serverLambdas.add(serverLambda);
        return this;
    }

    /**
     * Operator interface, for ad-hoc server configuration.
     */
    public interface ServerOperator {
        /**
         * Operation handler.
         *
         * @param config The server configuration.
         */
        void operation(HttpServer config);
    }
}
