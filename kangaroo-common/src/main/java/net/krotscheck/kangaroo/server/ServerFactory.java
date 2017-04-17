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

import org.apache.commons.configuration.Configuration;
import org.apache.http.client.utils.URIBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.ServletRegistration;
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
        URI serverUri = getServerUri(config);
        HttpServer server = createServer(serverUri);

        for (Entry<String, ResourceConfig> route : services.entrySet()) {
            String path = route.getKey();
            ResourceConfig rc = route.getValue();
            String name = route.getValue().getClass().getSimpleName();

            WebappContext context = new WebappContext(name, path);
            ServletRegistration registration =
                    context.addServlet(name, new ServletContainer(rc));
            registration.addMapping(String.format("%s/*", path));

            context.deploy(server);
        }

        return server;
    }

    /**
     * Create a new HTTP server for the provided URI.
     *
     * @param serverUri The URI to bind to.
     * @return An HTTP server, with no bound contexts.
     */
    private HttpServer createServer(final URI serverUri) {
        return GrizzlyHttpServerFactory.createHttpServer(serverUri, false);
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
