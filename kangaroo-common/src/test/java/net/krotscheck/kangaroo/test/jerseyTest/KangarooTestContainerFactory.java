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

package net.krotscheck.kangaroo.test.jerseyTest;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestHelper;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This container factory simulates, as closely as possible, a kangaroo
 * deployment for the provided context.
 *
 * @author Michael Krotscheck
 */
public final class KangarooTestContainerFactory
        implements TestContainerFactory {

    /**
     * Create a test container instance.
     *
     * @param baseUri base URI for the test container to run at.
     * @param context deployment context of the tested JAX-RS / Jersey
     *                application .
     * @return New test container configured to run the tested application.
     * @throws IllegalArgumentException if {@code deploymentContext} is not
     *                                  supported by this test container
     *                                  factory.
     */
    @Override
    public TestContainer create(final URI baseUri,
                                final DeploymentContext context) {
        if (!(context instanceof ServletDeploymentContext)) {
            throw new IllegalArgumentException("The deployment context must"
                    + " be an instance of ServletDeploymentContext.");
        }

        return new KangarooTestContainer(baseUri,
                (ServletDeploymentContext) context);
    }

    /**
     * This class has methods for instantiating, starting and stopping a
     * Grizzly-based web server.
     */
    private static final class KangarooTestContainer implements TestContainer {

        /**
         * Logger.
         */
        private static final Logger LOGGER =
                Logger.getLogger(KangarooTestContainer.class.getName());

        /**
         * The deployment context for this container.
         */
        private final ServletDeploymentContext deploymentContext;

        /**
         * Base URI, provided during initialization.
         */
        private URI baseUri;

        /**
         * The constructed HTTP server.
         */
        private HttpServer server;

        /**
         * Create a new container.
         *
         * @param baseUri The base deployment URI.
         * @param context The deployment context.
         */
        private KangarooTestContainer(final URI baseUri,
                                      final ServletDeploymentContext context) {
            this.baseUri = UriBuilder.fromUri(baseUri)
                    .path(context.getContextPath())
                    .path(context.getServletPath())
                    .build();

            LOGGER.info("Creating KangarooTestContainer configured"
                    + " at the base URI "
                    + TestHelper.zeroPortToAvailablePort(baseUri));

            this.deploymentContext = context;
            instantiateGrizzlyWebServer();
        }

        /**
         * Get any custom client configuration needed for the kangaroo server.
         *
         * @return A client configuration.
         */
        @Override
        public ClientConfig getClientConfig() {
            ClientConfig c = new ClientConfig();
            c.property(ClientProperties.FOLLOW_REDIRECTS, false);
            c.register(CsrfProtectionFilter.class);
            return c;
        }

        /**
         * Return the BaseURI. Required by clients.
         *
         * @return The base uri.
         */
        @Override
        public URI getBaseUri() {
            return baseUri;
        }

        /**
         * Start the container.
         */
        @Override
        public void start() {
            if (server.isStarted()) {
                LOGGER.log(Level.WARNING, "Ignoring start request"
                        + " - KangarooTestContainer is already started.");

            } else {
                LOGGER.log(Level.FINE, "Starting KangarooTestContainer...");
                try {
                    server.start();

                    if (baseUri.getPort() == 0) {
                        baseUri = UriBuilder.fromUri(baseUri)
                                .port(server.getListener("grizzly")
                                        .getPort())
                                .build();
                        LOGGER.log(Level.INFO, "Started KangarooTestContainer"
                                + " at the base URI " + baseUri);
                    }
                } catch (final IOException ioe) {
                    throw new TestContainerException(ioe);
                }
            }
        }

        /**
         * Stop the container.
         */
        @Override
        public void stop() {
            if (server.isStarted()) {
                LOGGER.log(Level.FINE, "Stopping KangarooTestContainer...");
                this.server.shutdownNow();
            } else {
                LOGGER.log(Level.WARNING, "Ignoring stop request"
                        + " - KangarooTestContainer is already stopped.");
            }
        }

        /**
         * Create a new instance of the Kangaroo Web Server.
         */
        private void instantiateGrizzlyWebServer() {

            String contextPathLocal = deploymentContext.getContextPath();
            if (!contextPathLocal.isEmpty()
                    && !contextPathLocal.startsWith("/")) {
                contextPathLocal = "/" + contextPathLocal;
            }

            String servletPathLocal = deploymentContext.getServletPath();
            if (!servletPathLocal.startsWith("/")) {
                servletPathLocal = "/" + servletPathLocal;
            }
            if (servletPathLocal.endsWith("/")) {
                servletPathLocal += "*";
            } else {
                servletPathLocal += "/*";
            }

            final WebappContext context =
                    new WebappContext("TestContext", contextPathLocal);

            // Servlet class, and servlet instance can be both null, or
            // one of them is specified exclusively.
            final HttpServlet servletInstance =
                    deploymentContext.getServletInstance();
            final Class<? extends HttpServlet> servletClass =
                    deploymentContext.getServletClass();

            if (servletInstance != null || servletClass != null) {
                final ServletRegistration registration;
                if (servletInstance != null) {
                    registration = context
                            .addServlet(servletInstance.getClass().getName(),
                                    servletInstance);
                } else {
                    registration = context.addServlet(servletClass.getName(),
                            servletClass);
                }
                registration.setInitParameters(
                        deploymentContext.getInitParams());
                registration.addMapping(servletPathLocal);
            }

            for (final Class<? extends EventListener> eventListener
                    : deploymentContext.getListeners()) {
                context.addListener(eventListener);
            }

            final Map<String, String> contextParams =
                    deploymentContext.getContextParams();
            for (final String contextParamName : contextParams.keySet()) {
                context.addContextInitParameter(contextParamName,
                        contextParams.get(contextParamName));
            }

            // Filter support
            if (deploymentContext.getFilters() != null) {
                for (final ServletDeploymentContext.FilterDescriptor
                        filterDescriptor : deploymentContext.getFilters()) {

                    final FilterRegistration filterRegistration =
                            context.addFilter(filterDescriptor.getFilterName(),
                                    filterDescriptor.getFilterClass());

                    filterRegistration.setInitParameters(
                            filterDescriptor.getInitParams());
                    filterRegistration.addMappingForUrlPatterns(
                            grizzlyDispatcherTypes(filterDescriptor
                                    .getDispatcherTypes()), true,
                            servletPathLocal);
                }
            }

            try {
                server = GrizzlyHttpServerFactory.createHttpServer(baseUri,
                        (GrizzlyHttpContainer) null,
                        false,
                        null,
                        false);
                context.deploy(server);
            } catch (final ProcessingException ex) {
                throw new TestContainerException(ex);
            }
        }

        /**
         * Recreate the dispatchers, so they're not reused.
         *
         * @param dispatcherTypes List of dispatchers.
         * @return Same enumset, different instances.
         */
        private EnumSet<DispatcherType> grizzlyDispatcherTypes(
                final Set<DispatcherType> dispatcherTypes) {
            final Set<DispatcherType> grizzlyDispatcherTypes = new HashSet<>();
            for (final javax.servlet.DispatcherType
                    dispatcherType : dispatcherTypes) {
                grizzlyDispatcherTypes
                        .add(DispatcherType.valueOf(dispatcherType.name()));
            }
            return EnumSet.copyOf(grizzlyDispatcherTypes);
        }
    }
}
