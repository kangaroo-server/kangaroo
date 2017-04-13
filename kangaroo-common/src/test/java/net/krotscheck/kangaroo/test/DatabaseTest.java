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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

/**
 * This test suite sets up a database, without a service container, to test
 * individual components that need to access the database.
 *
 * @author Michael Krotscheck
 */
public abstract class DatabaseTest {

    /**
     * The database test rule. Private, so it can be wrapped below.
     */
    private static final DatabaseResource DATABASE_RESOURCE =
            new DatabaseResource();

    /**
     * The hibernate test rule. Private, so it can be wrapped below.
     */
    public static final HibernateResource HIBERNATE_RESOURCE =
            new HibernateResource();

    /**
     * Ensure that a JDNI resource is set up for this suite.
     */
    @ClassRule
    public static final TestRule CLASS_RULES = RuleChain
            .outerRule(DATABASE_RESOURCE)
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
    private final ActiveSessions sessionCount =
            new ActiveSessions(HIBERNATE_RESOURCE);

    /**
     * Ensure that a JDNI resource is set up for this suite.
     */
    @Rule
    public final TestRule instanceRules = RuleChain
            .outerRule(testName)
            .around(sessionCount)
            .around(hibernate);

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    public final Session getSession() {
        return hibernate.getSession();
    }

    /**
     * Create and return a hibernate session factory the test database.
     *
     * @return The session factory
     */
    public final SessionFactory getSessionFactory() {
        return HIBERNATE_RESOURCE.getSessionFactory();
    }
}
