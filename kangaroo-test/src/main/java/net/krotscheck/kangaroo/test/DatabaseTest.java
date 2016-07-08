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
 */

package net.krotscheck.kangaroo.test;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;

/**
 * This test suite sets up a database, without a service container, to test
 * individual components that need to access the database.
 *
 * @author Michael Krotscheck
 */
public abstract class DatabaseTest implements IDatabaseTest {

    /**
     * The database management instance.
     */
    private static DatabaseManager manager = new DatabaseManager();

    /**
     * The list of loaded fixtures.
     */
    private List<IFixture> fixtures = new ArrayList<>();

    /**
     * Setup a database for our application.
     *
     * @throws Exception Initialization exception.
     */
    @BeforeClass
    public static void setupDatabaseSchema() throws Exception {
        manager.setupJNDI();
        manager.setupDatabase();
    }

    /**
     * Shut down the database.
     *
     * @throws Exception Teardown Exceptions.
     */
    @AfterClass
    public static void removeDatabaseSchema() throws Exception {
        manager.cleanDatabase();
    }

    /**
     * Set up the fixtures and any environment that's necessary.
     *
     * @throws Exception Thrown in the fixtures() interface.
     */
    @Before
    public final void setupData() throws Exception {
        List<IFixture> newFixtures = fixtures();
        if (newFixtures != null) {
            fixtures.addAll(newFixtures);
        }
        manager.buildSearchIndex();
    }

    /**
     * Cleanup the session factory after every run.
     *
     * @throws Exception An exception that indicates a failed data clear.
     */
    @After
    public final void clearData() throws Exception {
        fixtures.forEach(IFixture::clear);
        fixtures.clear();

        // Clean any outstanding sessions.
        manager.cleanSessions();
    }

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    @Override
    public final Session getSession() {
        return manager.getSession();
    }

    /**
     * Create and return a hibernate session factory the test database.
     *
     * @return The session factory
     */
    @Override
    public final SessionFactory getSessionFactory() {
        return manager.getSessionFactory();
    }
}
