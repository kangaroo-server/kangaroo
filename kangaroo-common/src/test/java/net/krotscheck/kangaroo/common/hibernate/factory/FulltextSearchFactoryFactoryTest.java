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

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Unit test for the fulltext search factory factory.
 *
 * @author Michael Krotscheck
 */
public final class FulltextSearchFactoryFactoryTest {

    /**
     * The jersey application handler.
     */
    private ApplicationHandler handler;

    /**
     * The jersey application service locator.
     */
    private ServiceLocator locator;

    /**
     * Setup the application handler for this test.
     */
    @Before
    public void setup() {
        ResourceConfig config = new ResourceConfig();
        config.register(TestFeature.class);
        handler = new ApplicationHandler(config);
        locator = handler.getServiceLocator();
    }

    /**
     * Teardown the application handler.
     */
    @After
    public void teardown() {
        locator.shutdown();
        locator = null;
        handler = null;
    }

    /**
     * Test provide and dispose.
     */
    @Test
    public void testProvideDispose() {
        SessionFactory sessionFactory =
                locator.getService(SessionFactory.class);
        Session hibernateSession = sessionFactory.openSession();
        FullTextSession ftSession = Search.getFullTextSession(hibernateSession);


        FulltextSearchFactoryFactory factory =
                new FulltextSearchFactoryFactory(ftSession);

        // Make sure that we can create a search factory.
        SearchFactory searchFactory = factory.provide();
        Assert.assertNotNull(searchFactory);

        // Make sure we can dispose of the factory (does nothing, sadly).
        factory.dispose(searchFactory);

        if (hibernateSession.isOpen()) {
            hibernateSession.close();
        }
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
            context.register(new FulltextSessionFactory.Binder());
            context.register(new FulltextSearchFactoryFactory.Binder());
            return true;
        }
    }
}
