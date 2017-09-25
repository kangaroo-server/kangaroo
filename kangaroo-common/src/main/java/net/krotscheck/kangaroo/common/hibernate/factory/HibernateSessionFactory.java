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

package net.krotscheck.kangaroo.common.hibernate.factory;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This factory builds Hibernate Sessions on a per-request basis, disposing of
 * them after the request has been processed. This is to ensure that sessions
 * are disposed of and your configured threadpool is never oversaturated. To
 * use, merely {Inject} a new {Session} into your Jersey2 component using the
 * HK2 annotations.
 *
 * @author Michael Krotscheck
 */
public final class HibernateSessionFactory
        implements DisposableSupplier<Session> {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(HibernateSessionFactory.class);

    /**
     * The session factory, provided by the injection context.
     */
    private SessionFactory factory;

    /**
     * Create a new instance of the hibernate session factory injector.
     *
     * @param sessionFactory The regular hibernate session factory to wrap.
     */
    @Inject
    public HibernateSessionFactory(final SessionFactory sessionFactory) {
        this.factory = sessionFactory;
    }

    /**
     * Create a brand new session.
     *
     * @return A new, unused hibernate session.
     */
    @Override
    public Session get() {
        logger.trace("Creating hibernate session.");
        return factory.openSession();
    }

    /**
     * Dispose of a hibernate session.
     *
     * @param session The session to dispose of.
     */
    @Override
    public void dispose(final Session session) {
        if (session != null && session.isConnected()) {
            logger.trace("Disposing of hibernate session.");
            session.close();
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(HibernateSessionFactory.class)
                    .to(Session.class)
                    .to(SessionImpl.class)
                    .named("root_session")
                    .in(RequestScoped.class);
        }
    }
}
