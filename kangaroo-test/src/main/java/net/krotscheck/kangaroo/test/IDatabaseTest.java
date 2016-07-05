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
import org.junit.Before;

import java.io.File;
import java.util.List;

/**
 * Interface for a test class that provides a preloaded database.
 *
 * @author Michael Krotscheck
 */
public interface IDatabaseTest {

    /**
     * Set up the fixtures and any environment that's necessary.
     *
     * @throws Exception An exception that indicates a failed data load.
     */
    @Before
    void setupData() throws Exception;

    /**
     * Cleanup the session factory after every run.
     *
     * @throws Exception An exception that indicates a failed data clear.
     */
    @After
    void clearData() throws Exception;

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception An exception that indicates a failed fixture load.
     */
    List<IFixture> fixtures() throws Exception;

    /**
     * Load the test data.
     *
     * @return The test data.
     * @throws Exception An exception that indicates a failed test data load.
     */
    File testData() throws Exception;

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    Session getSession();

    /**
     * Create and return a hibernate session factory the test database.
     *
     * @return The session factory
     */
    SessionFactory getSessionFactory();

}
