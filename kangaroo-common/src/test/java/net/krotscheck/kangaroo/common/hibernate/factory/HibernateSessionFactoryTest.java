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
import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import net.krotscheck.kangaroo.test.rule.WorkingDirectoryRule;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
     * Ensure that we have a working directory.
     */
    @Rule
    public final TestRule workingDirectory = new WorkingDirectoryRule();

    /**
     * The jersey application injector.
     */
    private InjectionManager injector;

    /**
     * Setup the application handler for this test.
     */
    @Before
    public void setUp() {
        injector = Injections.createInjectionManager();
        injector.register(new SystemConfiguration.Binder());
        injector.register(new HibernateServiceRegistryFactory.Binder());
        injector.register(new HibernateSessionFactoryFactory.Binder());
        injector.register(new HibernateSessionFactory.Binder());
        injector.completeRegistration();
    }

    /**
     * Teardown the application handler.
     */
    @After
    public void tearDown() {
        injector.shutdown();
        injector = null;
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
        assertNotNull(session);
        assertTrue(session.isOpen());

        // Make sure we can dispose of the session.
        factory.dispose(session);
        assertFalse(session.isOpen());

        // Make sure that disposing an already closed session doesn't blow up.
        factory.dispose(session);
        assertFalse(session.isOpen());

        // Make sure accidentally passing null doesn't blow up.
        factory.dispose(null);
    }
}
