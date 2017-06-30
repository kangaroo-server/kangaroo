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
 *
 */

package net.krotscheck.kangaroo.test;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.core.Application;

/**
 * A single extension point class for all of our jersey tests.
 *
 * @author Michael Krotscheck.
 */
public abstract class KangarooJerseyTest extends JerseyTest {

    /**
     * Ask the test to construct an application, and then inject this test
     * into the context.
     *
     * @return The application itself
     */
    @Override
    protected final Application configure() {
        forceSet(TestProperties.CONTAINER_PORT, "0");
        forceSet(TestProperties.LOG_TRAFFIC, "true");
        forceSet(TestProperties.DUMP_ENTITY, "true");

        ResourceConfig config = createApplication();
        config.register(this);

        return config;
    }

    /**
     * Rules applicable to each test.
     */
    @Rule
    public final TestRule instanceRules = Timeout.seconds(10);

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
    }

    /**
     * Install the JUL-to-SLF4J logging bridge, before any application is
     * bootstrapped. This explicitly pre-empts the same code in the logging
     * feature, to catch test-initialization related log messages from
     * jerseytest.
     */
    @BeforeClass
    public static void installLogging() {
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }
}
