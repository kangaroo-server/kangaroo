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

package net.krotscheck.kangaroo.authz.oauth2.session.tasks;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.type.Type;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.TimerTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit test the HTTP Session cleanup task.
 *
 * @author Michael Krotscheck
 */
public final class HttpSessionCleanupTaskTest extends DatabaseTest {

    /**
     * The application context used for this test suite.
     */
    private static ApplicationContext context;

    /**
     * Preload data into the system.
     */
    @ClassRule
    public static final TestDataResource TEST_DATA_RESOURCE =
            new TestDataResource(HIBERNATE_RESOURCE) {
                @Override
                protected void loadTestData(final Session session) {
                    context = ApplicationBuilder.newApplication(session)
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Test)
                            .user()
                            .identity()
                            .build();
                }

            };

    /**
     * Assert that the task can be executed with expected results.
     */
    @Test
    public void testGetTask() {
        HttpSessionCleanupTask task =
                new HttpSessionCleanupTask(getSessionFactory(), null);
        TimerTask rTask = task.getTask();
        assertSame(task, rTask);
    }

    /**
     * Assert that the task can be executed with expected results.
     */
    @Test
    public void testSimpleRun() {
        // A series of 10 test tokens, 5 of which are expired and 5 of which
        // are not.
        ApplicationBuilder b = context.getBuilder();
        for (int i = 0; i < 10; i++) {
            b.httpSession(i % 2 == 1);
        }
        b.build();

        HttpSessionCleanupTask task =
                new HttpSessionCleanupTask(getSessionFactory(), null);
        TimerTask rTask = task.getTask();
        Query q = getSession()
                .createQuery("SELECT count(id) from HttpSession");

        long startCount = (long) q.uniqueResult();
        assertEquals((long) 10, startCount);
        rTask.run();
        long count = (long) q.uniqueResult();
        assertEquals((long) 5, count);

        // Run one more time, make sure it doesn't change anything.
        rTask.run();
        long secondCount = (long) q.uniqueResult();
        assertEquals((long) 5, secondCount);
    }

    /**
     * Assert that if the cleanup throws an exception, the resources are
     * still released.
     */
    @Test
    public void testRunWithError() {
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        Session mockSession = Mockito.mock(Session.class);
        Query mockQuery = Mockito.mock(Query.class);
        Transaction mockTransaction = Mockito.mock(Transaction.class);

        doReturn(mockQuery)
                .when(mockQuery)
                .setParameter(Matchers.anyString(),
                        Matchers.any(),
                        Matchers.any(Type.class));
        doThrow(HibernateException.class)
                .when(mockQuery)
                .executeUpdate();
        doReturn(mockQuery)
                .when(mockSession)
                .createQuery(Matchers.anyString());
        doReturn(mockSession)
                .when(mockFactory)
                .openSession();
        doReturn(mockTransaction)
                .when(mockSession)
                .beginTransaction();

        HttpSessionCleanupTask task =
                new HttpSessionCleanupTask(mockFactory, null);
        TimerTask rTask = task.getTask();
        rTask.run();

        verify(mockTransaction, Mockito.times(1)).rollback();
        verify(mockSession, Mockito.times(1)).close();
    }

    /**
     * Assert that if an unexpected error occurs, we still close the session.
     */
    @Test(expected = SQLException.class)
    public void testRunWithUnexpectedError() {
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        Session mockSession = Mockito.mock(Session.class);
        Query mockQuery = Mockito.mock(Query.class);
        Transaction mockTransaction = Mockito.mock(Transaction.class);

        doReturn(mockQuery)
                .when(mockQuery)
                .setParameter(Matchers.anyString(),
                        Matchers.any(),
                        Matchers.any(Type.class));
        doThrow(SQLException.class)
                .when(mockQuery)
                .executeUpdate();
        doReturn(mockQuery)
                .when(mockSession)
                .createQuery(Matchers.anyString());
        doReturn(mockSession)
                .when(mockFactory)
                .openSession();
        doReturn(mockTransaction)
                .when(mockSession)
                .beginTransaction();

        HttpSessionCleanupTask task =
                new HttpSessionCleanupTask(mockFactory, null);
        TimerTask rTask = task.getTask();
        rTask.run();

        verify(mockTransaction, Mockito.times(1)).rollback();
        verify(mockSession, Mockito.times(1)).close();
    }

    /**
     * Make sure that if transaction rollback fails, an the session is still
     * closed.
     */
    @Test
    public void assertSessionClosedOnRollbackFailure() {
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        Session mockSession = Mockito.mock(Session.class);
        Query mockQuery = Mockito.mock(Query.class);
        Transaction mockTransaction = Mockito.mock(Transaction.class);

        doReturn(mockQuery)
                .when(mockQuery)
                .setParameter(Matchers.anyString(),
                        Matchers.any(),
                        Matchers.any(Type.class));
        doReturn(0)
                .when(mockQuery)
                .executeUpdate();
        doReturn(mockQuery)
                .when(mockSession)
                .createQuery(Matchers.anyString());
        doReturn(mockSession)
                .when(mockFactory)
                .openSession();
        doReturn(mockTransaction)
                .when(mockSession)
                .beginTransaction();

        HttpSessionCleanupTask task =
                new HttpSessionCleanupTask(mockFactory, null);
        TimerTask rTask = task.getTask();
        rTask.run();

        verify(mockTransaction, Mockito.times(1)).commit();
        verify(mockSession, Mockito.times(1)).close();
    }

    /**
     * Make sure that if session closure fails, we rethrow.
     */
    @Test(expected = HibernateException.class)
    public void assertSessionClosedErrorRethrown() {
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        Session mockSession = Mockito.mock(Session.class);
        Query mockQuery = Mockito.mock(Query.class);
        Transaction mockTransaction = Mockito.mock(Transaction.class);

        doReturn(mockQuery)
                .when(mockQuery)
                .setParameter(Matchers.anyString(),
                        Matchers.any(),
                        Matchers.any(Type.class));
        doReturn(0)
                .when(mockQuery)
                .executeUpdate();
        doReturn(mockQuery)
                .when(mockSession)
                .createQuery(Matchers.anyString());
        doReturn(mockSession)
                .when(mockFactory)
                .openSession();
        doReturn(mockTransaction)
                .when(mockSession)
                .beginTransaction();
        doThrow(HibernateException.class)
                .when(mockSession)
                .close();

        HttpSessionCleanupTask task =
                new HttpSessionCleanupTask(mockFactory, null);
        TimerTask rTask = task.getTask();
        rTask.run();
    }

    /**
     * Assert that the tick is every 10 minutes.
     */
    @Test
    public void getPeriod() {
        HttpSessionCleanupTask task =
                new HttpSessionCleanupTask(getSessionFactory(), null);
        assertEquals(10 * 60 * 1000, task.getPeriod());
    }

    /**
     * Assert that the delay is zero.
     */
    @Test
    public void getDelay() {
        HttpSessionCleanupTask task =
                new HttpSessionCleanupTask(getSessionFactory(), null);
        // By default, the period should equal the delay. No
        // immediately-running tasks.
        assertEquals(task.getPeriod(), task.getDelay());
    }
}
