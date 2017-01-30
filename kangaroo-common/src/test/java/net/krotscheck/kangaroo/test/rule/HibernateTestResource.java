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

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This rule exists to bootstrap the hibernate constructs necessary to
 * properly isolate a test. It provides sessions, query builders, and wraps
 * the entire test in a transaction.
 *
 * @author Michael Krotscheck
 */
public class HibernateTestResource implements TestRule {

    /**
     * The source of our hibernate session factory.
     */
    private final HibernateResource factoryProvider;

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
     * Create a new instance of the hibernate session rule.
     *
     * @param factoryProvider The source of the session factory.
     */
    public HibernateTestResource(final HibernateResource factoryProvider) {
        this.factoryProvider = factoryProvider;
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
                createHibernateConnection();
                startTransaction();
                try {
                    base.evaluate();
                } finally {
                    closeTransaction();
                    closeHibernateConnection();
                }
            }
        };
    }

    /**
     * Start a transaction.
     */
    private void startTransaction() {
        getSession().beginTransaction();
    }

    /**
     * Close the transaction, if necessary.
     */
    private void closeTransaction() {
        Transaction t = getSession().getTransaction();
        try {
            if (t.getStatus().equals(TransactionStatus.ACTIVE)) {
                t.commit();
            }
        } catch (HibernateException he) {
            t.rollback();

            // Rethrow, because we're in tests.
            throw he;
        }
    }

    /**
     * Ensure that the hibernate connection is closed even if a test fails.
     */
    private void closeHibernateConnection() {
        searchFactory = null;

        if (fullTextSession.isOpen()) {
            fullTextSession.close();
        }
        fullTextSession = null;

        // Clean any outstanding sessions.
        if (session.isOpen()) {
            session.close();
        }
        session = null;
    }

    /**
     * Create a session factory for the database.
     */
    private void createHibernateConnection() {
        session = factoryProvider.getSessionFactory().openSession();
        fullTextSession = Search.getFullTextSession(session);
        searchFactory = fullTextSession.getSearchFactory();
    }
}
