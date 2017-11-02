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
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Unit test for the fulltext search factory factory.
 *
 * @author Michael Krotscheck
 */
public final class FulltextSearchFactoryFactoryTest {

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
    public void setup() {
        injector = Injections.createInjectionManager();
        injector.register(new SystemConfiguration.Binder());
        injector.register(new HibernateServiceRegistryFactory.Binder());
        injector.register(new HibernateSessionFactoryFactory.Binder());
        injector.register(new HibernateSessionFactory.Binder());
        injector.register(new FulltextSessionFactory.Binder());
        injector.register(new FulltextSearchFactoryFactory.Binder());
        injector.completeRegistration();
    }

    /**
     * Teardown the application handler.
     */
    @After
    public void teardown() {
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
        Session hibernateSession = sessionFactory.openSession();
        FullTextSession ftSession = Search.getFullTextSession(hibernateSession);


        FulltextSearchFactoryFactory factory =
                new FulltextSearchFactoryFactory(ftSession);

        // Make sure that we can create a search factory.
        SearchFactory searchFactory = factory.get();
        Assert.assertNotNull(searchFactory);

        if (hibernateSession.isOpen()) {
            hibernateSession.close();
        }
    }
}
