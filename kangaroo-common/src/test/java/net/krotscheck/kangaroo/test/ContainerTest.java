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

import net.krotscheck.kangaroo.test.rule.ActiveSessions;
import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import net.krotscheck.kangaroo.test.rule.HibernateResource;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * A test suite sets up an entire application container to run our tests
 * against.
 *
 * @author Michael Krotscheck
 */
public abstract class ContainerTest extends KangarooJerseyTest {

    /**
     * A list of HTTP status codes that are valid for redirects.
     */
    private static final List<Integer> VALID_REDIRECT_CODES =
            Arrays.asList(HttpStatus.SC_SEE_OTHER, HttpStatus.SC_CREATED,
                    HttpStatus.SC_MOVED_PERMANENTLY,
                    HttpStatus.SC_MOVED_TEMPORARILY);

    /**
     * The database test rule. Private, so it can be wrapped below.
     */
    private static final DatabaseResource DATABASE_RESOURCE =
            new DatabaseResource();

    /**
     * The hibernate test rule. Private, so it can be wrapped below.
     */
    private static final HibernateResource HIBERNATE_RESOURCE =
            new HibernateResource();

    /**
     * Make the test name available during a test.
     */
    private static final TestName TEST_NAME = new TestName();

    /**
     * Make the # of active DB sessions available in every test.
     */
    private static final ActiveSessions SESSION_COUNT = new ActiveSessions();

    /**
     * Ensure that a JDNI resource is set up for this suite.
     */
    @ClassRule
    public static final TestRule RULES = RuleChain
            .outerRule(TEST_NAME)
            .around(DATABASE_RESOURCE)
            .around(SESSION_COUNT)
            .around(HIBERNATE_RESOURCE);

    /**
     * Mark the # of sessions that exist.
     */
    @BeforeClass
    public static void markSessionCount() {
        SESSION_COUNT.mark();
    }

    /**
     * Ensure that all sessions have been cleaned up.
     */
    @AfterClass
    public static final void enforceSessionCount() {
        Assert.assertFalse("Zombie DB sessions detected.",
                SESSION_COUNT.check()
        );
    }

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    public final Session getSession() {
        return HIBERNATE_RESOURCE.getSession();
    }

    /**
     * Create and return a hibernate session factory the test database.
     *
     * @return The session factory
     */
    public final SessionFactory getSessionFactory() {
        return HIBERNATE_RESOURCE.getSessionFactory();
    }

    /**
     * Retrieve the search factory for the test.
     *
     * @return The session factory
     */
    public final SearchFactory getSearchFactory() {
        return HIBERNATE_RESOURCE.getSearchFactory();
    }

    /**
     * Retrieve the fulltext session for the test.
     *
     * @return The session factory
     */
    public final FullTextSession getFullTextSession() {
        return HIBERNATE_RESOURCE.getFullTextSession();
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
