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

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * A test suite sets up an entire application container to run our tests
 * against.
 *
 * @author Michael Krotscheck
 */
public abstract class ContainerTest
        extends JerseyTest
        implements IDatabaseTest {

    /**
     * The database management instance.
     */
    private static DatabaseManager manager = new DatabaseManager();

    /**
     * The list of loaded fixtures.
     */
    private List<IFixture> fixtures = new ArrayList<>();

    /**
     * Configure all jersey2 clients.
     *
     * @param config The configuration instance to modify.
     */
    @Override
    protected final void configureClient(final ClientConfig config) {
        config.property(ClientProperties.FOLLOW_REDIRECTS, false);
    }

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
     */
    @After
    public final void clearData() {
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

    /**
     * This is a convenience method which, presuming a response constitutes a
     * redirect, will follow that redirect and return the subsequent response.
     *
     * @param original The original response, which should constitute a
     *                 redirect.
     * @return The response.
     */
    protected final Response followRedirect(final Response original) {

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, original.getStatus());

        URI uri = original.getLocation();
        WebTarget target = target().path(uri.getPath());

        // Iterate through the query parameters and apply them.
        for (NameValuePair pair : URLEncodedUtils.parse(uri, "UTF-8")) {
            target = target.queryParam(pair.getName(), pair.getValue());
        }

        return target.request().get();
    }
}
