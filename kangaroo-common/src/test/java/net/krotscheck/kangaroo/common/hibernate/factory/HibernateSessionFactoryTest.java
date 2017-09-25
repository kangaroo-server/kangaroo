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

import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * The hibernate session factory test.
 *
 * @author Michael Krotscheck
 */
public final class HibernateSessionFactoryTest {

    /**
     * Ensure that the JNDI Resource exists.
     */
    @ClassRule
    public static final TestRule DATABASE = new DatabaseResource();

    /**
     * The jersey application handler.
     */
    private ApplicationHandler handler;

    /**
     * The jersey application injector.
     */
    private InjectionManager injector;

    /**
     * Setup the application handler for this test.
     */
    @Before
    public void setup() {
        ResourceConfig config = new ResourceConfig();
        config.register(TestFeature.class);
        handler = new ApplicationHandler(config);
        injector = Injections.createInjectionManager();
        injector.register(new HibernateServiceRegistryFactory.Binder());
        injector.register(new HibernateSessionFactory.Binder());
        injector.register(new HibernateSessionFactoryFactory.Binder());
    }

    /**
     * Teardown the application handler.
     */
    @After
    public void teardown() {
        injector.shutdown();
        injector = null;
        handler = null;
    }

    /**
     * Test provide and dispose.
     */
    @Test
    public void testProvideDispose() {
        SessionFactory sessionFactory =
                injector.getInstance(SessionFactory.class);

        HibernateSessionFactory factory =
                new HibernateSessionFactory(sessionFactory);

        // Make sure that we can create a session.
        Session session = factory.get();
        Assert.assertNotNull(session);
        Assert.assertTrue(session.isOpen());

        // Make sure we can dispose of the session.
        factory.dispose(session);
        Assert.assertFalse(session.isOpen());

        // Make sure that disposing an already closed session doesn't blow up.
        factory.dispose(session);
        Assert.assertFalse(session.isOpen());

        // Make sure accidentally passing null doesn't blow up.
        factory.dispose(null);
    }

    /**
     * A private class to test our feature injection.
     */
    private static class TestFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext context) {
            context.register(new HibernateServiceRegistryFactory.Binder());
            context.register(new HibernateSessionFactoryFactory.Binder());
            context.register(new HibernateSessionFactory.Binder());
            return true;
        }
    }
}
