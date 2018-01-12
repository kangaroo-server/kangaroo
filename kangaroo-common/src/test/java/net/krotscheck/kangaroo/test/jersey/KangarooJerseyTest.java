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

package net.krotscheck.kangaroo.test.jersey;

import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A single extension point class for all of our jersey tests.
 *
 * @author Michael Krotscheck.
 */
public abstract class KangarooJerseyTest extends JerseyTest {

    /**
     * Install the JUL-to-SLF4J logging bridge, before any application is
     * bootstrapped.
     */
    static {
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }

    /**
     * Rules applicable to each test.
     */
    @Rule
    public final TestRule instanceRules = Timeout.seconds(10);

    /**
     * Override system properties necessary for testing.
     */
    @BeforeClass
    public static void overrideSystemProperties() {
        System.setProperty("hibernate.c3p0.min_size", "0");
        System.setProperty("hibernate.c3p0.max_size", "5");
    }

    /**
     * Cleanup system property overrides.
     */
    @AfterClass
    public static void cleanupSystemProperties() {
        System.clearProperty("hibernate.c3p0.min_size");
        System.clearProperty("hibernate.c3p0.max_size");
    }

    /**
     * Use the grizzly web container factory, instead of the default one.
     * This allows us to simulate a full Servlet environment.
     *
     * @return The container factory.
     */
    @Override
    protected TestContainerFactory getTestContainerFactory() {
        KangarooTestContainerFactory factory =
                new KangarooTestContainerFactory();
        factory.configureServer((server) -> {
            ServerConfiguration c = server.getServerConfiguration();
            c.setSessionTimeoutSeconds(1000);
        });

        return factory;
    }

    /**
     * Configure the deployment as a servlet.
     *
     * @return The deployment context.
     */
    @Override
    protected DeploymentContext configureDeployment() {
        forceSet(TestProperties.CONTAINER_PORT, "0");
        forceSet(TestProperties.LOG_TRAFFIC, "true");
        forceSet(TestProperties.DUMP_ENTITY, "true");

        return ServletDeploymentContext.forServlet(
                new ServletContainer(createApplication()))
                .initParam("swagger.context.id", "TestContext")
                .build();
    }

    /**
     * Create an application.
     *
     * @return The application to test.
     */
    protected abstract ResourceConfig createApplication();

    /**
     * Configure all jersey2 clients.
     *
     * @param config The configuration instance to modify.
     */
    @Override
    protected final void configureClient(final ClientConfig config) {
        config.property(ClientProperties.FOLLOW_REDIRECTS, false);
        config.register(CsrfProtectionFilter.class);
        config.register(JacksonFeature.class);
    }
}
