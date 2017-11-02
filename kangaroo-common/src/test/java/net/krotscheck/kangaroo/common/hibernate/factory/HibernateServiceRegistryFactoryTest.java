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

package net.krotscheck.kangaroo.common.hibernate.factory;


import net.krotscheck.kangaroo.common.config.SystemConfiguration;
import net.krotscheck.kangaroo.server.Config;
import net.krotscheck.kangaroo.test.TestConfig;
import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.service.ServiceRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.ArrayList;

/**
 * Unit test for the hibernate configuration and its binder.
 *
 * @author Michael Krotscheck
 */
public final class HibernateServiceRegistryFactoryTest {

    /**
     * Ensure that the JNDI Resource exists.
     */
    @ClassRule
    public static final TestRule DATABASE = new DatabaseResource();

    /**
     * Setup the test.
     */
    @Before
    public void setupTest() {
        System.setProperty(Config.WORKING_DIR.getKey(), "./target");
    }

    /**
     * Teardown the test.
     */
    @After
    public void teardownTest() {
        System.clearProperty(Config.WORKING_DIR.getKey());
    }

    /**
     * Test provide and dispose.
     */
    @Test
    public void testProvideDispose() {
        SystemConfiguration testConfig =
                new SystemConfiguration(new ArrayList<>());
        HibernateServiceRegistryFactory factory = new
                HibernateServiceRegistryFactory(testConfig);

        ServiceRegistry serviceRegistry = factory.get();

        String dbDriver = TestConfig.getDbDriver();
        Dialect d = new MetadataSources(serviceRegistry)
                .buildMetadata()
                .getDatabase()
                .getDialect();

        switch (dbDriver) {
            case "org.h2.Driver":
                Assert.assertTrue(d instanceof H2Dialect);
                break;
            case "org.mariadb.jdbc.Driver":
                Assert.assertTrue(d instanceof MariaDBDialect);
                break;
            default:
                Assert.fail(String.format("Unrecognized driver: %s", dbDriver));
        }

        // Dispose of the registry.
        factory.dispose(serviceRegistry);
    }

    /**
     * Test the application binder.
     *
     * @throws ClassNotFoundException Thrown if the driver class is not found.
     */
    @Test
    public void testBinder() throws ClassNotFoundException {

        // Create a fake injector.
        InjectionManager injector = Injections.createInjectionManager();
        injector.register(new SystemConfiguration.Binder());
        injector.register(new HibernateServiceRegistryFactory.Binder());
        injector.completeRegistration();

        ServiceRegistry serviceRegistry =
                injector.getInstance(ServiceRegistry.class);
        Assert.assertNotNull(serviceRegistry);

        // Make sure it's reading from the same place.
        String dbDriver = TestConfig.getDbDriver();
        Dialect d = new MetadataSources(serviceRegistry)
                .buildMetadata().getDatabase().getDialect();

        switch (dbDriver) {
            case "org.h2.Driver":
                Assert.assertTrue(d instanceof H2Dialect);
                break;
            case "org.mariadb.jdbc.Driver":
                Assert.assertTrue(d instanceof MariaDBDialect);
                break;
            default:
                Assert.fail(String.format("Unrecognized driver: %s", dbDriver));
        }

        // Make sure it's a singleton...
        ServiceRegistry serviceRegistry2 =
                injector.getInstance(ServiceRegistry.class);
        Assert.assertSame(serviceRegistry, serviceRegistry2);

        injector.shutdown();
    }

    /**
     * A private class to test our feature injection.
     */
    private static class TestFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext context) {
            context.register(new HibernateServiceRegistryFactory.Binder());
            return true;
        }
    }
}
