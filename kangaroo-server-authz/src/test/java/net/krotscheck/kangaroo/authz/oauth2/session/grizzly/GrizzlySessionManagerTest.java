/*
 * Copyright (c) 2017 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.oauth2.session.grizzly;

import net.krotscheck.kangaroo.authz.AuthzServerConfig;
import net.krotscheck.kangaroo.authz.common.database.entity.HttpSession;
import net.krotscheck.kangaroo.common.config.SystemConfiguration;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Session;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for our Grizzly session manager.
 *
 * @author Michael Krotscheck
 */
public final class GrizzlySessionManagerTest extends DatabaseTest {

    /**
     * Manager under test.
     */
    private GrizzlySessionManager manager;

    /**
     * Setup the test.
     */
    @Before
    public void setup() {
        SystemConfiguration config =
                new SystemConfiguration(Collections.emptyList());
        manager = new GrizzlySessionManager(config, this::getSessionFactory);
    }

    /**
     * Test creating a session.
     */
    @Test
    public void testCreateSession() {
        Session newSession = manager.createSession(null);

        // Assert that this session exists in the database.
        org.hibernate.Session hSession = getSession();
        HttpSession dbSession = hSession.get(HttpSession.class,
                IdUtil.fromString(newSession.getIdInternal()));

        ensureSessionsMatch(dbSession, newSession);
    }

    /**
     * Test a simple "Get session" action.
     */
    @Test
    public void testGetSession() {
        org.hibernate.Session hSession = getSession();
        HttpSession s = new HttpSession();
        s.setSessionTimeout(1000);

        Calendar zero = Calendar.getInstance();
        zero.setTimeInMillis(0);

        // Create the session and zero the dates.
        hSession.beginTransaction();
        hSession.save(s);
        Query q = hSession.createQuery("update HttpSession set "
                + "createdDate=?, modifiedDate=?");
        q.setParameter(0, zero);
        q.setParameter(1, zero);
        q.executeUpdate();
        hSession.evict(s);
        hSession.getTransaction().commit();

        Session result = manager.getSession(null,
                IdUtil.toString(s.getId()));

        // Clear the whole session...
        hSession.clear();

        hSession.beginTransaction();
        s = hSession.get(HttpSession.class, s.getId());
        hSession.getTransaction().commit();

        // Make sure that modified is greater than created
        assertTrue(s.getModifiedDate().getTimeInMillis() > 0);

        ensureSessionsMatch(s, result);
    }

    /**
     * If no id is passed, do nothing.
     */
    @Test
    public void testGetSessionNoId() {
        assertNull(manager.getSession(null, null));
    }

    /**
     * If the requested ID doesn't exist, do nothing.
     */
    @Test
    public void testNonexistentSession() {
        String testId = IdUtil.toString(IdUtil.next());
        assertNull(manager.getSession(null, testId));
    }

    /**
     * Assert that we can change a session ID.
     */
    @Test
    public void testChangeSessionId() {
        org.hibernate.Session hSession = getSession();
        HttpSession s = new HttpSession();
        s.setSessionTimeout(1000);

        hSession.beginTransaction();
        hSession.save(s);
        hSession.getTransaction().commit();
        hSession.evict(s);

        Session browserSession = manager.asSession(s);

        String oldId = manager.changeSessionId(null, browserSession);

        assertEquals(s.getId(), IdUtil.fromString(oldId));

        hSession.beginTransaction();
        HttpSession testSession = hSession.get(HttpSession.class, s.getId());
        hSession.getTransaction().commit();
        assertNull(testSession);

        assertNotEquals(oldId, browserSession.getIdInternal());

        hSession.beginTransaction();
        HttpSession newSession = hSession.get(HttpSession.class,
                IdUtil.fromString(browserSession.getIdInternal()));
        hSession.getTransaction().commit();
        assertNotNull(newSession);

        assertEquals(s.getSessionTimeout(), newSession.getSessionTimeout());
        assertNotEquals(s.getCreatedDate(), newSession.getCreatedDate());
        assertNotEquals(s.getModifiedDate(), newSession.getModifiedDate());
    }

    /**
     * Assert that changing an ID, if no original exists, does nothing.
     */
    @Test
    public void testChangeSessionIdNoOriginal() {
        HttpSession s = new HttpSession();
        s.setId(IdUtil.next());
        s.setCreatedDate(Calendar.getInstance());
        s.setModifiedDate(Calendar.getInstance());
        s.setSessionTimeout(1000);

        Session browserSession = manager.asSession(s);

        String oldId = manager.changeSessionId(null, browserSession);

        assertEquals(s.getId(), IdUtil.fromString(oldId));
    }

    /**
     * Assert that throwing in the change ID block is cleanly caught.
     */
    @Test
    public void testChangeSessionIdThrown() {
        SystemConfiguration config =
                new SystemConfiguration(Collections.emptyList());
        SessionFactory mockFactory = mock(SessionFactory.class);
        org.hibernate.Session mockHSession = mock(org.hibernate.Session.class);
        Transaction mockTransaction = mock(Transaction.class);

        doReturn(mockHSession).when(mockFactory).openSession();
        doReturn(mockTransaction).when(mockHSession).getTransaction();
        doThrow(HibernateException.class).when(mockHSession).beginTransaction();

        manager = new GrizzlySessionManager(config, () -> mockFactory);

        HttpSession s = new HttpSession();
        s.setId(IdUtil.next());
        s.setCreatedDate(Calendar.getInstance());
        s.setModifiedDate(Calendar.getInstance());
        s.setSessionTimeout(1000);

        Session browserSession = manager.asSession(s);

        String oldId = manager.changeSessionId(null, browserSession);

        verify(mockHSession, times(1)).close();
        verify(mockTransaction, times(1)).rollback();

        assertEquals(s.getId(), IdUtil.fromString(oldId));
    }

    /**
     * Assert that we can get/set the session cookie name.
     */
    @Test
    public void testGetSetSessionCookieName() {
        String oldName = manager.getSessionCookieName();
        manager.setSessionCookieName("new_cookie_name");

        assertEquals("new_cookie_name", manager.getSessionCookieName());
        assertEquals("kangaroo", oldName);
    }

    /**
     * Assert that we can convert from entity to session.
     */
    @Test
    public void testAsSession() {
        assertNull(manager.asSession(null));

        HttpSession s = new HttpSession();
        s.setId(IdUtil.next());
        s.setCreatedDate(Calendar.getInstance());
        s.setModifiedDate(Calendar.getInstance());
        s.setSessionTimeout(1000);

        Session result = manager.asSession(s);
        ensureSessionsMatch(s, result);
    }

    /**
     * Assert that cookie parameters are set when called for.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testConfigureSessionCookie() throws Exception {
        Request r = mock(Request.class);
        doReturn(new StringBuilder("http://example.com/foo"))
                .when(r)
                .getRequestURL();

        Cookie c = new Cookie("JSESSIONID", null);

        manager.configureSessionCookie(r, c);

        assertEquals("example.com", c.getDomain());
        assertEquals(1, c.getVersion());
        assertTrue(c.isHttpOnly());
        assertTrue(c.isSecure());
        assertEquals(AuthzServerConfig.SESSION_NAME.getValue(),
                c.getName());
        assertEquals(AuthzServerConfig.SESSION_MAX_AGE.getValue(),
                Integer.valueOf(c.getMaxAge()));
    }

    /**
     * Comparison helper method.
     *
     * @param dbSession      The DB session to match.
     * @param browserSession The browser session to match.
     */
    private void ensureSessionsMatch(final HttpSession dbSession,
                                     final Session browserSession) {
        assertEquals(IdUtil.toString(dbSession.getId()),
                browserSession.getIdInternal());
        assertEquals(dbSession.getCreatedDate().getTimeInMillis(),
                browserSession.getCreationTime());
        assertEquals(dbSession.getModifiedDate().getTimeInMillis(),
                browserSession.getTimestamp());
        assertEquals(dbSession.getSessionTimeout(),
                browserSession.getSessionTimeout());
    }
}
