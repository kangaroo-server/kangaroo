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

package net.krotscheck.kangaroo.test.rule;

import net.krotscheck.kangaroo.common.hibernate.factory.HibernateServiceRegistryFactory;
import net.krotscheck.kangaroo.common.hibernate.listener.CreatedUpdatedListener;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SystemConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.ServiceRegistry;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This JUnit Rule bootstraps hibernate so that it is bound to the correct
 * testing database. It also provides a session factory, though it does not
 * presume to create or destroy individual sessions or transactions.
 *
 * @author Michael Krotscheck
 */
public final class HibernateResource implements TestRule {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(HibernateResource.class);

    /**
     * Override system configuration.
     */
    private final Configuration systemConfiguration
            = new SystemConfiguration();

    /**
     * Service registry, constructed from the configuration.
     */
    private ServiceRegistry registry;

    /**
     * Internal session factory, reconstructed for every test run.
     */
    private SessionFactory sessionFactory;

    /**
     * A new hiberate resource.
     */
    public HibernateResource() {
    }

    /**
     * Create and return a hibernate session factory the test database.
     *
     * @return The session factory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Modifies the method-running {@link Statement} to implement this
     * test-running rule.
     *
     * @param base        The {@link Statement} to be modified
     * @param description A {@link Description} of the test implemented in
     *                    {@code base}
     * @return a new statement, which may be the same as {@code base},
     * a wrapper around {@code base}, or a completely new Statement.
     */
    @Override
    public Statement apply(final Statement base,
                           final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                initializeSearchIndexProvider();
                createHibernateConnection();
                try {
                    base.evaluate();
                } finally {
                    closeHibernateConnection();
                    resetSearchIndexProvider();
                }
            }
        };
    }

    /**
     * Override any settings automatically loaded from the hibernate
     * configuration file.
     */
    private void initializeSearchIndexProvider() {
        System.setProperty("hibernate.search.default.exclusive_index_use",
                "false");
    }

    /**
     * Clear any settings we've previously set.
     */
    private void resetSearchIndexProvider() {
        System.clearProperty("hibernate.search.default.exclusive_index_use");
    }

    /**
     * Ensure that the hibernate connection is closed even if a test fails.
     *
     * @throws IOException Thrown if we cannot clean up.
     */
    private void closeHibernateConnection() throws IOException {
        if (!sessionFactory.isClosed()) {
            logger.debug("Closing SessionFactory");
            sessionFactory.close();
        }
        sessionFactory = null;

        logger.debug("Disposing ServiceRegistry");
        new HibernateServiceRegistryFactory(systemConfiguration)
                .dispose(registry);
    }

    /**
     * Create a session factory for the database.
     *
     * @throws IOException Thrown if we cannot clean up.
     */
    private void createHibernateConnection() throws IOException {
        // Create the session factory.
        logger.debug("Creating ServiceRegistry");
        registry = new HibernateServiceRegistryFactory(systemConfiguration)
                .get();

        logger.debug("Creating SessionFactory");
        sessionFactory = new MetadataSources(registry)
                .buildMetadata()
                .buildSessionFactory();

        logger.debug("Injecting event listeners");
        EventListenerRegistry eventRegistry =
                ((SessionFactoryImpl) sessionFactory)
                        .getServiceRegistry()
                        .getService(EventListenerRegistry.class);
        eventRegistry.appendListeners(EventType.PRE_INSERT,
                new CreatedUpdatedListener());
        eventRegistry.appendListeners(EventType.PRE_UPDATE,
                new CreatedUpdatedListener());
    }
}
