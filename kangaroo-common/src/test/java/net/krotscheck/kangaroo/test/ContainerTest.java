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
import net.krotscheck.kangaroo.test.rule.HibernateTestResource;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Logger instance.
     */
    private static Logger logger = LoggerFactory.getLogger(ContainerTest.class);

    /**
     * A list of HTTP status codes that are valid for redirects.
     */
    private static final List<Integer> VALID_REDIRECT_CODES =
            Arrays.asList(HttpStatus.SC_SEE_OTHER, HttpStatus.SC_CREATED,
                    HttpStatus.SC_MOVED_PERMANENTLY,
                    HttpStatus.SC_MOVED_TEMPORARILY);

    /**
     * The hibernate rule, explicitly created so we can reference it later.
     */
    public static final HibernateResource HIBERNATE_RESOURCE =
            new HibernateResource();

    /**
     * Ensure that a JDNI resource is set up for this suite.
     */
    @ClassRule
    public static final TestRule CLASS_RULES = RuleChain
            .outerRule(new DatabaseResource())
            .around(HIBERNATE_RESOURCE);

    /**
     * The hibernate test rule. Private, so it can be wrapped below.
     */
    private final HibernateTestResource hibernate =
            new HibernateTestResource(HIBERNATE_RESOURCE);

    /**
     * Make the test name available during a test.
     */
    private final TestName testName = new TestName();

    /**
     * Make the # of active DB sessions available in every test.
     */
    private final ActiveSessions sessionCount = new ActiveSessions();

    /**
     * Ensure that a JDNI resource is set up for this suite.
     */
    @Rule
    public final TestRule instanceRules = RuleChain
            .outerRule(testName)
            .around(sessionCount)
            .around(hibernate);

    /**
     * Log out the test name.
     */
    @Before
    public final void logTestName() {
        logger.info(testName.getMethodName());
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
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    public final Session getSession() {
        return hibernate.getSession();
    }

    /**
     * Retrieve the search factory for the test.
     *
     * @return The session factory
     */
    public final SearchFactory getSearchFactory() {
        return hibernate.getSearchFactory();
    }

    /**
     * Retrieve the fulltext session for the test.
     *
     * @return The session factory
     */
    public final FullTextSession getFullTextSession() {
        return hibernate.getFullTextSession();
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
