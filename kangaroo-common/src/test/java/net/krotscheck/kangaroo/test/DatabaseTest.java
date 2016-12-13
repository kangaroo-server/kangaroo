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
import net.krotscheck.kangaroo.test.rule.HibernateResource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
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
    private static final HibernateResource HIBERNATE_RESOURCE =
            new HibernateResource();

    /**
     * Ensure that a JDNI resource is set up for this suite.
     */
    @ClassRule
    public static final TestRule RULES = RuleChain
            .outerRule(DATABASE_RESOURCE)
            .around(HIBERNATE_RESOURCE);

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
}
