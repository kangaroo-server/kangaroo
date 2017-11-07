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

package net.krotscheck.kangaroo.common.hibernate.lifecycle;

import net.krotscheck.kangaroo.common.hibernate.migration.DatabaseMigrationState;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.impl.ImplementationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This lifecycle listener will rebuild the hibernate search index based on the
 * configured connector available in hibernate.cfg.xml.
 *
 * @author Michael Krotscheck
 */
public final class SearchIndexContainerLifecycleListener
        implements ContainerLifecycleListener {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(SearchIndexContainerLifecycleListener.class);

    /**
     * The session factory.
     */
    private final SessionFactory sessionFactory;

    /**
     * The migration state.
     */
    @SuppressWarnings("PMD")
    private final DatabaseMigrationState migrationState;

    /**
     * Create a new instance of the lifecycle listener, with an
     * injected registry.
     *
     * @param sessionFactory Hibernate session factory, provided
     *                       by the injector.
     * @param migrationState Indicator about whether
     *                       the database was migrated.
     */
    @Inject
    public SearchIndexContainerLifecycleListener(
            final SessionFactory sessionFactory,
            final DatabaseMigrationState migrationState) {
        this.sessionFactory = sessionFactory;
        this.migrationState = migrationState;
    }

    /**
     * On startup, ensure that the search index has been built, but only if
     * the schema changes.
     *
     * @param container The container that is starting.
     */
    @Override
    public void onStartup(final Container container) {
        if (!migrationState.isSchemaChanged()) {
            logger.debug("Schema did not change, aborting rebuild...");
            return;
        }

        logger.debug("Rebuilding Search Index...");
        Session s = sessionFactory.openSession();
        try {
            // Create the fulltext session.
            FullTextSession fullTextSession = getFulltextSession(s);

            fullTextSession
                    .createIndexer()
                    .startAndWait();
        } catch (InterruptedException e) {
            logger.warn("Search reindex interrupted. Good luck!");
            logger.trace("Error:", e);
        } finally {
            s.close();
        }
    }

    /**
     * When the container reloads, do nothing.
     *
     * @param container The container that is restarting.
     */
    @Override
    public void onReload(final Container container) {
        // Do nothing.
    }

    /**
     * On shutdown, do nothing.
     *
     * @param container The container that is shutting down.
     */
    @Override
    public void onShutdown(final Container container) {
        // Do nothing
    }

    /**
     * Build a full text session.
     *
     * @param s The session to wrap.
     * @return A new FullTextSession
     */
    public FullTextSession getFulltextSession(final Session s) {
        return ImplementationFactory.createFullTextSession(s);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(SearchIndexContainerLifecycleListener.class)
                    .to(ContainerLifecycleListener.class)
                    .in(Singleton.class);
        }
    }
}
