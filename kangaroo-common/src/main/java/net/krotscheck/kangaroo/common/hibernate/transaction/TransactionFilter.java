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

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

/**
 * This container filter will start, and close/rollback, a hibernate session
 * during a regular request flow.
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.USER)
@Transactional
public final class TransactionFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * The request's session provider.
     */
    private final Provider<Session> sessionProvider;

    /**
     * Create a new instance of this authorization filter.
     *
     * @param sessionProvider The context-relevant session provider.
     */
    @Inject
    public TransactionFilter(final Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    /**
     * Before a request gets processed, start a transaction.
     *
     * @param requestContext request context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext)
            throws IOException {
        Session s = sessionProvider.get();
        s.getTransaction().begin();
    }

    /**
     * Commit the transaction, or roll it back if necessary.
     *
     * @param requestContext  request context.
     * @param responseContext response context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext)
            throws IOException {
        Session s = sessionProvider.get();
        Transaction t = s.getTransaction();

        try {
            if (t.getStatus().equals(TransactionStatus.ACTIVE)) {
                s.getTransaction().commit();
            }
        } catch (HibernateException he) {
            t.rollback();
            Response r = ErrorResponseBuilder
                    .from(he)
                    .build();
            responseContext
                    .setStatus(Status.INTERNAL_SERVER_ERROR.getStatusCode());
            responseContext.setEntity(r.getEntity());
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(TransactionFilter.class)
                    .to(ContainerRequestFilter.class)
                    .to(ContainerResponseFilter.class)
                    .in(Singleton.class);
        }
    }
}
