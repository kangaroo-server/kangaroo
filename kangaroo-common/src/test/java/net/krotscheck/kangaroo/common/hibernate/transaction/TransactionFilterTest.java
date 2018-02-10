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

package net.krotscheck.kangaroo.common.hibernate.transaction;

import net.krotscheck.kangaroo.common.hibernate.transaction.TransactionFilter.Binder;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import javax.inject.Provider;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.krotscheck.kangaroo.test.jersey.BinderAssertion.assertBinderContains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

/**
 * Unit tests for the transactionalization filter.
 *
 * @author Michael Krotscheck
 */
public final class TransactionFilterTest {

    /**
     * Mock session provider.
     */
    private Provider<Session> mockSessionProvider;

    /**
     * The mock session.
     */
    private Session mockSession;

    /**
     * The mock transaction.
     */
    private Transaction mockTransaction;

    /**
     * Mock request context.
     */
    private ContainerRequestContext requestContext;

    /**
     * Mock request context.
     */
    private ContainerResponseContext responseContext;

    /**
     * Setup the provider.
     */
    @Before
    public void setUp() {
        mockTransaction = Mockito.mock(Transaction.class);
        mockSession = Mockito.mock(Session.class);
        mockSessionProvider = (Provider<Session>) Mockito.mock(Provider.class);
        requestContext = Mockito.mock(ContainerRequestContext.class);
        responseContext = Mockito.mock(ContainerResponseContext.class);

        Mockito.doReturn(mockSession).when(mockSessionProvider).get();
        Mockito.doReturn(mockTransaction).when(mockSession).getTransaction();
        Mockito.doReturn(mockTransaction).when(mockSession).getTransaction();
        Mockito.doReturn(TransactionStatus.ACTIVE).when(mockTransaction)
                .getStatus();
    }

    /**
     * Test that a transaction is opened on the request filter.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testRequestFilter() throws Exception {
        TransactionFilter filter = new TransactionFilter(mockSessionProvider);
        filter.filter(requestContext);

        Mockito.verifyNoMoreInteractions(requestContext);
        Mockito.verify(mockTransaction, times(1)).begin();
    }

    /**
     * Test that a transaction is closed on the response filter.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testResponseFilter() throws Exception {
        TransactionFilter filter = new TransactionFilter(mockSessionProvider);
        filter.filter(requestContext, responseContext);

        Mockito.verifyNoMoreInteractions(requestContext);
        Mockito.verifyNoMoreInteractions(responseContext);
        Mockito.verify(mockTransaction, times(1)).commit();
    }

    /**
     * Test that committing all the non-active states does not result in a
     * transaction commit.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testResponseFilterNoActiveState() throws Exception {
        List<TransactionStatus> statuses =
                Arrays.stream(TransactionStatus.values())
                        .filter(t -> !t.equals(TransactionStatus.ACTIVE))
                        .collect(Collectors.toList());


        for (TransactionStatus s : statuses) {
            doReturn(s).when(mockTransaction).getStatus();

            TransactionFilter
                    filter = new TransactionFilter(mockSessionProvider);
            filter.filter(requestContext, responseContext);

            Mockito.verifyNoMoreInteractions(requestContext);
            Mockito.verifyNoMoreInteractions(responseContext);
            Mockito.verify(mockTransaction, times(0)).commit();
        }
    }

    /**
     * Test that a transaction is rolled back if the response filter
     * encounters an error.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testResponseFilterError() throws Exception {
        Mockito.doThrow(HibernateException.class)
                .when(mockTransaction)
                .commit();

        TransactionFilter filter = new TransactionFilter(mockSessionProvider);
        filter.filter(requestContext, responseContext);

        Mockito.verifyNoMoreInteractions(requestContext);
        Mockito.verify(mockTransaction, times(1)).commit();
        Mockito.verify(mockTransaction, times(1)).rollback();

        Mockito.verify(responseContext, times(1))
                .setEntity(Matchers.any());
        Mockito.verify(responseContext, times(1))
                .setStatus(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        Mockito.verify(responseContext, times(1))
                .setEntity(Matchers.any());

        Mockito.verifyNoMoreInteractions(responseContext);
    }

    /**
     * Assert that we can inject values using this binder.
     */
    @Test
    public void testBinder() {
        assertBinderContains(new Binder(),
                ContainerRequestFilter.class,
                ContainerResponseFilter.class);
    }
}
