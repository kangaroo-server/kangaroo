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

package net.krotscheck.kangaroo.common.hibernate.context;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This context listener will rebuild the hibernate search index based on the
 * configured connector available in hibernate.cfg.xml. It is provided as a
 * servlet context listener (rather than a container context listener), as
 * deploying multiple containers may be deployed in the same artifact. This way,
 * the index is only reconstructed once.
 *
 * @author Michael Krotscheck
 */
public final class SearchIndexContextListener
        implements ServletContextListener {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(SearchIndexContextListener.class);

    /**
     * Create a session factory given the current configuration method.
     *
     * @return A constructed session factory.
     */
    protected SessionFactory createSessionFactory() {
        // Set up a service registry.
        StandardServiceRegistryBuilder b = new StandardServiceRegistryBuilder();

        // configures settings from hibernate.cfg.xml
        ServiceRegistry registry = b.configure().build();

        // Build the session factory.
        return new MetadataSources(registry)
                .buildMetadata()
                .buildSessionFactory();
    }

    /**
     * Rebuild the search index during context initialization.
     *
     * @param servletContextEvent The context event (not really used).
     */
    @Override
    public void contextInitialized(
            final ServletContextEvent servletContextEvent) {
        logger.info("Rebuilding Search Index...");

        // Build the session factory.
        SessionFactory factory = createSessionFactory();

        // Build the hibernate session.
        Session session = factory.openSession();

        // Create the fulltext session.
        FullTextSession fullTextSession = Search.getFullTextSession(session);

        try {
            fullTextSession
                    .createIndexer()
                    .startAndWait();
        } catch (InterruptedException e) {
            logger.warn("Search reindex interrupted. Good luck!");
            logger.trace("Error:", e);
        } finally {
            // Close everything and release the lock file.
            session.close();
            factory.close();
        }
    }

    @Override
    public void contextDestroyed(
            final ServletContextEvent servletContextEvent) {
        // Do nothing.
    }
}
