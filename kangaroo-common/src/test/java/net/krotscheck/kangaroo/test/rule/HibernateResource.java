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
import net.krotscheck.kangaroo.test.rule.hibernate.TestDirectoryProvider;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.service.ServiceRegistry;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This JUnit Rule creates and maintains a set of Hibernate entities that
 * match the configuration settings for the application database itself.
 *
 * @author Michael Krotscheck
 */
public class HibernateResource implements TestRule {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(HibernateResource.class);

    /**
     * Service registry, constructed from the configuration.
     */
    private ServiceRegistry registry;

    /**
     * Internal session factory, reconstructed for every test run.
     */
    private SessionFactory sessionFactory;

    /**
     * The last created session.
     */
    private Session session;

    /**
     * Search factory.
     */
    private SearchFactory searchFactory;

    /**
     * Fulltext session.
     */
    private FullTextSession fullTextSession;

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Retrieve the search factory for the test.
     *
     * @return The session factory
     */
    public final SearchFactory getSearchFactory() {
        return searchFactory;
    }

    /**
     * Retrieve the fulltext session for the test.
     *
     * @return The session factory
     */
    public final FullTextSession getFullTextSession() {
        return fullTextSession;
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
                overrideSettings();
                createHibernateConnection();
                try {
                    base.evaluate();
                } finally {
                    closeHibernateConnection();
                    clearSettings();
                }
            }
        };
    }

    /**
     * Override any settings automatically loaded from the hibernate
     * configuration file.
     */
    private void overrideSettings() {
        // Override the cache directory implementation setting for hibernate
        // search. This provider is essentially a RAMDirectory, except that
        // it can be accessed by all the different SessionFactories
        // instantiated during a full test.
        String name = TestDirectoryProvider.class.getTypeName();
        System.setProperty("hibernate.search.default.directory_provider",
                name);
        System.setProperty("hibernate.search.default.exclusive_index_use",
                "false");
    }

    /**
     * Clear any settings we've previously set.
     */
    private void clearSettings() {
        System.clearProperty("hibernate.search.default.directory_provider");
        System.clearProperty("hibernate.search.default.exclusive_index_use");
    }

    /**
     * Ensure that the hibernate connection is closed even if a test fails.
     */
    private void closeHibernateConnection() {
        searchFactory = null;

        if (fullTextSession.isOpen()) {
            logger.debug("Closing FullTextSession");
            fullTextSession.close();
        }
        fullTextSession = null;

        // Clean any outstanding sessions.
        if (session.isOpen()) {
            logger.debug("Closing Session");
            session.close();
        }
        session = null;

        if (!sessionFactory.isClosed()) {
            logger.debug("Closing SessionFactory");
            sessionFactory.close();
        }
        sessionFactory = null;

        logger.debug("Disposing ServiceRegistry");
        new HibernateServiceRegistryFactory().dispose(registry);
    }

    /**
     * Create a session factory for the database.
     */
    private void createHibernateConnection() {
        // Create the session factory.
        logger.debug("Creating ServiceRegistry");
        registry = new HibernateServiceRegistryFactory().provide();

        logger.debug("Creating SessionFactory");
        sessionFactory = new MetadataSources(registry)
                .buildMetadata()
                .buildSessionFactory();
        logger.debug("Opening Session");
        session = sessionFactory.openSession();

        logger.debug("Creating FullTextSession");
        fullTextSession = Search.getFullTextSession(session);
        searchFactory = fullTextSession.getSearchFactory();
    }
}
