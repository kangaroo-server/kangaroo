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
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.inject.Inject;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

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
     * A list of HTTP status codes that are valid for redirects.
     */
    private static final List<Integer> VALID_REDIRECT_CODES =
            Arrays.asList(HttpStatus.SC_SEE_OTHER, HttpStatus.SC_CREATED,
                    HttpStatus.SC_MOVED_PERMANENTLY,
                    HttpStatus.SC_MOVED_TEMPORARILY);

    /**
     * The database management instance.
     */
    @ClassRule
    public static final DatabaseResource JNDI = new DatabaseResource();

    /**
     * The list of loaded fixtures.
     */
    private List<EnvironmentBuilder> fixtures = new ArrayList<>();

    /**
     * Session factory, injected by Jersey2.
     */
    private SessionFactory sessionFactory;

    /**
     * Search factory, injected by Jersey2.
     */
    private SearchFactory searchFactory;

    /**
     * Fulltext session, injected by jersey2.
     */
    private FullTextSession fullTextSession;

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
        fullTextSession = Search.getFullTextSession(session);
        searchFactory = fullTextSession.getSearchFactory();

        List<EnvironmentBuilder> newFixtures = fixtures();
        if (newFixtures != null) {
            fixtures.addAll(newFixtures);
        }
    }

    /**
     * Cleanup the session factory after every run.
     */
    @After
    public final void clearData() {
        Query clearOwners = session.createQuery("UPDATE Application SET " +
                "owner=null");
        Query clearApplications = session.createQuery("DELETE from " +
                "Application");

        // Delete everything, making sure to remove the cyclic reference on
        // owners first.
        Transaction t = session.beginTransaction();
        clearOwners.executeUpdate();
        clearApplications.executeUpdate();
        t.commit();

        fixtures.clear();

        session.close();
        searchFactory = null;
        fullTextSession = null;
        session = null;
    }

    /**
     * Install the JUL-to-SLF4J logging bridge, before any application is
     * bootstrapped. This explicitly pre-empts the same code in the logging
     * feature, to catch test-initialization related log messages from
     * jerseytest.
     */
    @BeforeClass
    public static void installLogging() {
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
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
     * This is a convenience method which, presuming a response constitutes a
     * redirect, will follow that redirect and return the subsequent response.
     *
     * @param original The original response, which should constitute a
     *                 redirect.
     * @return The response.
     */
    protected final Response followRedirect(final Response original) {
        return followRedirect(original, null);
    }

    /**
     * This is a convenience method which, presuming a response constitutes a
     * redirect, will follow that redirect and return the subsequent response.
     *
     * @param original   The original response, which should constitute a
     *                   redirect.
     * @param authHeader An optional authorization header.
     * @return The response.
     */
    protected final Response followRedirect(final Response original,
                                            final String authHeader) {

        assertTrue(VALID_REDIRECT_CODES.contains(original.getStatus()));

        URI uri = original.getLocation();
        WebTarget target = target().path(uri.getPath());

        // Iterate through the query parameters and apply them.
        for (NameValuePair pair : URLEncodedUtils.parse(uri, "UTF-8")) {
            target = target.queryParam(pair.getName(), pair.getValue());
        }

        Builder b = target.request();
        if (authHeader != null) {
            b.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return b.get();
    }
}
