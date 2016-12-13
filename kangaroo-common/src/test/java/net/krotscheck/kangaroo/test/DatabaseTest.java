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

package net.krotscheck.kangaroo.test;

import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This test suite sets up a database, without a service container, to test
 * individual components that need to access the database.
 *
 * @author Michael Krotscheck
 */
public abstract class DatabaseTest {

    /**
     * Ensure that a JDNI resource is set up for this suite.
     */
    @ClassRule
    public static final DatabaseResource JNDI = new DatabaseResource();

    /**
     * Internal session factory, reconstructed for every test run.
     */
    private SessionFactory sessionFactory;

    /**
     * The last created session.
     */
    private Session session;

    /**
     * The list of loaded fixtures.
     */
    private List<EnvironmentBuilder> fixtures = new ArrayList<>();

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception An exception that indicates a failed fixture load.
     */
    public abstract List<EnvironmentBuilder> fixtures() throws Exception;

    /**
     * Set up the fixtures and any environment that's necessary.
     *
     * @throws Exception Thrown in the fixtures() interface.
     */
    @Before
    public final void setupData() throws Exception {
        // Create the session factory.
        ServiceRegistry serviceRegistry =
                new StandardServiceRegistryBuilder()
                        .configure()
                        .build();
        sessionFactory = new MetadataSources(serviceRegistry)
                .buildMetadata()
                .buildSessionFactory();

        // Create the session
        session = sessionFactory.openSession();

        List<EnvironmentBuilder> newFixtures = fixtures();
        if (newFixtures != null) {
            fixtures.addAll(newFixtures);
        }
    }

    /**
     * Cleanup the session factory after every run.
     *
     * @throws Exception An exception that indicates a failed data clear.
     */
    @After
    public final void clearData() throws Exception {
        List<EnvironmentBuilder> reversed = new ArrayList<>(fixtures);
        Collections.reverse(reversed);
        reversed.forEach(EnvironmentBuilder::clear);
        fixtures.clear();

        // Clean any outstanding sessions.
        session.close();
        session = null;

        sessionFactory.close();
        sessionFactory = null;
    }

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    public final Session getSession() {
        return session;
    }

    /**
     * Create and return a hibernate session factory the test database.
     *
     * @return The session factory
     */
    public final SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}
