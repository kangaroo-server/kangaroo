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

import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
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
    @ClassRule
    public static final DatabaseResource JNDI = new DatabaseResource();

    /**
     * The list of loaded fixtures.
     */
    private List<IFixture> fixtures = new ArrayList<>();

    /**
     * Session factory, injected by Jersey2.
     */
    private SessionFactory sessionFactory;

    /**
     * Session specifically for this test, cleaned up after each test.
     */
    private Session session;

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
     * Ask the test to construct an application, and then inject this test
     * into the context.
     *
     * @return The application itself
     */
    @Override
    protected final Application configure() {
        ResourceConfig config = createApplication();
        config.register(this);

        return config;
    }

    /**
     * Create an application.
     *
     * @return The application to test.
     */
    protected abstract ResourceConfig createApplication();

    /**
     * Set up the fixtures and any environment that's necessary.
     *
     * @throws Exception Thrown in the fixtures() interface.
     */
    @Before
    public final void setupData() throws Exception {
        session = sessionFactory.openSession();

        List<IFixture> newFixtures = fixtures();
        if (newFixtures != null) {
            fixtures.addAll(newFixtures);
        }
    }

    /**
     * Cleanup the session factory after every run.
     */
    @After
    public final void clearData() {
        List<IFixture> reversed = new ArrayList<>(fixtures);
        Collections.reverse(reversed);
        reversed.forEach(IFixture::clear);
        fixtures.clear();

        // Clear the session.
        session.close();
        session = null;
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
     * Set the injected session factory.
     *
     * @param sessionFactory The new session factory from the jersey context.
     */
    @Inject
    public final void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Create and return a hibernate session factory the test database.
     *
     * @return The session factory
     */
    @Override
    public final SessionFactory getSessionFactory() {
        return sessionFactory;
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
