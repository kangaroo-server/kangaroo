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

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This factory creates a request-scoped FulltextSession from the provided
 * hibernate session.
 *
 * @author Michael Krotscheck
 */
public final class FulltextSessionFactory implements Factory<FullTextSession> {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(FulltextSessionFactory.class);

    /**
     * Our singleton session factory, injected.
     */
    private Session hibernateSession;

    /**
     * Create a new fulltext session factory.
     *
     * @param session The hibernate session from which to read our resources.
     */
    @Inject
    public FulltextSessionFactory(final Session session) {
        this.hibernateSession = session;
    }

    /**
     * Create a new fulltext session.
     *
     * @return A fulltext session attached to the current hibernate session.
     */
    @Override
    public FullTextSession provide() {
        logger.trace("Creating hibernate fulltext session.");
        return Search.getFullTextSession(hibernateSession);
    }

    /**
     * Dispose of the fulltext session if it hasn't already been closed.
     *
     * @param session The fulltext session to dispose of.
     */
    @Override
    public void dispose(final FullTextSession session) {
        if (session != null && session.isOpen()) {
            logger.trace("Disposing of hibernate fulltext session.");
            session.close();
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(FulltextSessionFactory.class)
                    .to(FullTextSession.class)
                    .in(RequestScoped.class);
        }
    }
}
