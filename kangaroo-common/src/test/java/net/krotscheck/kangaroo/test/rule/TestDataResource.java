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
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A rule which, by extension, can act as a test data loader for any test.
 *
 * @author Michael Krotscheck
 */
public abstract class TestDataResource implements TestRule {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(TestDataResource.class);

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

                try (SessionFactory f = buildSessionFactory();
                     Session s = f.openSession()) {
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
     * Create a session factory for the database.
     */
    private SessionFactory buildSessionFactory() {
        // Create the session factory.
        logger.debug("Creating ServiceRegistry");
        ServiceRegistry registry =
                new StandardServiceRegistryBuilder()
                        .configure()
                        .applySettings(System.getProperties())
                        .build();
        logger.debug("Creating SessionFactory");
        return new MetadataSources(registry)
                .buildMetadata()
                .buildSessionFactory();
    }


    /**
     * Load data into the database.
     *
     * @param session The hibernate session to use to persist our data.
     */
    protected abstract void loadTestData(Session session);

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

        Transaction t = session.beginTransaction();
        removeOwners.executeUpdate();
        removeApplications.executeUpdate();
        removeConfiguration.executeUpdate();
        t.commit();
    }
}
