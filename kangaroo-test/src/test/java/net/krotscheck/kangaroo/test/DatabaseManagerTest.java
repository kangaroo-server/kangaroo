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
import org.junit.Assert;
import org.junit.Test;

/**
 * Smoke tests for the database manager, in order to get coverage so that
 * coveralls won't yell at us.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseManagerTest {

    /**
     * Walk through the whole lifecycle. This isn't really intended to test
     * anything, as we don't really ship this (it's only used in tests).
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testManagerLifecycle() throws Exception {
        DatabaseManager m = new DatabaseManager();

        m.setupJNDI();
        m.setupJNDI(); // Run it again, to ensure the exception is caught.
        m.setupDatabase();

        SessionFactory f1 = m.getSessionFactory();
        Session s1 = m.getSession();
        SessionFactory f2 = m.getSessionFactory();
        Session s2 = m.getSession();

        Assert.assertSame(s1, s2);
        Assert.assertSame(f1, f2);
        Assert.assertFalse(f1.isClosed());
        Assert.assertTrue(s2.isOpen());

        m.cleanSessions();
        m.cleanDatabase();

        // Call twice, to make sure it doesn't error.
        m.cleanSessions();
        m.cleanDatabase();
    }
}
