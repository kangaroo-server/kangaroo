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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import javax.inject.Inject;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
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
@Deprecated
public abstract class DContainerTest
        extends KangarooJerseyTest {

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
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception An exception that indicates a failed fixture load.
     */
    public abstract List<EnvironmentBuilder> fixtures() throws Exception;

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
        List<EnvironmentBuilder> reversed = new ArrayList<>(fixtures);
        Collections.reverse(reversed);
        reversed.forEach(EnvironmentBuilder::clear);
        fixtures.clear();

        session.close();
        searchFactory = null;
        fullTextSession = null;
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
