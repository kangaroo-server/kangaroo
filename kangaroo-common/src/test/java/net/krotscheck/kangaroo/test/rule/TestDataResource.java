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

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A rule which, by extension, can act as a test data loader for any test.
 *
 * @author Michael Krotscheck
 */
public abstract class TestDataResource implements TestRule {

    /**
     * The source of our hibernate session factory.
     */
    private final HibernateResource factoryProvider;

    /**
     * Create a new instance of the test data resource.
     *
     * @param factoryProvider The session factory provider.
     */
    public TestDataResource(final HibernateResource factoryProvider) {
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
    public final Statement apply(final Statement base,
                                 final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {

                SessionFactory factory = factoryProvider.getSessionFactory();
                try (Session s = factory.openSession()) {
                    loadTestData(s);
                    try {
                        base.evaluate();
                    } finally {
                        clearTestData(s);
                    }
                }
            }
        };
    }


    /**
     * Load data into the database.
     *
     * @param session The hibernate session to use to persist our data.
     */
    protected abstract void loadTestData(Session session);

    /**
     * Return the session factory associated with this resource.
     *
     * @return The driving session factory, provided by the constructor.
     */
    protected final SessionFactory getSessionFactory() {
        return factoryProvider.getSessionFactory();
    }

    /**
     * Wipe the database clean.
     *
     * @param session The hibernate session to use to persist our data.
     */
    private void clearTestData(final Session session) {
        Query removeOwners =
                session.createQuery("UPDATE Application SET owner = null");
        Query removeApplications =
                session.createQuery("DELETE FROM Application");
        Query removeConfiguration =
                session.createQuery("DELETE FROM ConfigurationEntry");

        session.getTransaction().begin();
        removeOwners.executeUpdate();
        removeApplications.executeUpdate();
        removeConfiguration.executeUpdate();
        session.getTransaction().commit();
    }
}
