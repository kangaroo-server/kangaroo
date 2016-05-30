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

package net.krotscheck.test;

import org.glassfish.jersey.test.JerseyTest;
import org.hibernate.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;

/**
 * A test suite sets up an entire application container to run our tests
 * against.
 *
 * @author Michael Krotscheck
 */
public abstract class ContainerTest extends JerseyTest {

    /**
     * The database management instance.
     */
    private static DatabaseManager manager = new DatabaseManager();

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
        manager.clearTestData();
        manager.cleanDatabase();
    }

    /**
     * Cleanup the session factory after every run.
     */
    @After
    public final void clearSession() {
        manager.cleanSessions();
    }

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    protected final Session getSession() {
        return manager.getSession();
    }

    /**
     * Load some test data into our database.
     *
     * @param testData The test data xml file to map.
     */
    public static void loadTestData(final File testData) {
        manager.loadTestData(testData);
    }
}
