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


import net.krotscheck.kangaroo.test.DatabaseTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.List;

/**
 * Unit test for the hibernate configuration and its binder.
 *
 * @author Michael Krotscheck
 */
public final class HibernateServiceRegistryFactoryTest extends DatabaseTest {

    /**
     * Test provide and dispose.
     */
    @Test
    public void testProvideDispose() {
        HibernateServiceRegistryFactory factory = new
                HibernateServiceRegistryFactory();

        ServiceRegistry serviceRegistry = factory.provide();

        Dialect d = new MetadataSources(serviceRegistry)
                .buildMetadata().getDatabase().getDialect();
        Assert.assertNotNull(d);

        // This shouldn't actually do anything, but is included here for
        // coverage.
        factory.dispose(serviceRegistry);
    }

    /**
     * Test the application binder.
     */
    @Test
    public void testBinder() {

        ResourceConfig config = new ResourceConfig();
        config.register(TestFeature.class);

        // Make sure it's registered
        Assert.assertTrue(config.isRegistered(TestFeature.class));

        // Create a fake application.
        ApplicationHandler handler = new ApplicationHandler(config);
        ServiceRegistry serviceRegistry = handler
                .getServiceLocator().getService(ServiceRegistry.class);
        Assert.assertNotNull(serviceRegistry);

        // Make sure it's reading from the same place.
        Dialect d = new MetadataSources(serviceRegistry)
                .buildMetadata().getDatabase().getDialect();
        Assert.assertNotNull(d);

        // Make sure it's a singleton...
        ServiceRegistry serviceRegistry2 = handler
                .getServiceLocator().getService(ServiceRegistry.class);
        Assert.assertSame(serviceRegistry, serviceRegistry2);
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception An exception that indicates a failed fixture load.
     */
    @Override
    public List<EnvironmentBuilder> fixtures(final Session session)
            throws Exception {
        return null;
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
