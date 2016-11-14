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

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;

import javax.inject.Inject;

/**
 * Abstract implementation of our common services.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractService {

    /**
     * Hibernate session.
     */
    private final Session session;

    /**
     * The hibernate search factory.
     */
    private final SearchFactory searchFactory;

    /**
     * The hibernate fulltext session.
     */
    private final FullTextSession fullTextSession;

    /**
     * Create a new instance of the application service.
     *
     * @param session         The Hibernate session.
     * @param searchFactory   The FT Search factory.
     * @param fullTextSession The fulltext search factory.
     */
    @Inject
    public AbstractService(final Session session,
                           final SearchFactory searchFactory,
                           final FullTextSession fullTextSession) {
        this.session = session;
        this.searchFactory = searchFactory;
        this.fullTextSession = fullTextSession;
    }

    /**
     * Get the session.
     *
     * @return The injected session.
     */
    protected final Session getSession() {
        return session;
    }

    /**
     * Retrieve the search factory.
     *
     * @return The injected search factory.
     */
    protected final SearchFactory getSearchFactory() {
        return searchFactory;
    }

    /**
     * Get the full text search session.
     *
     * @return The injected lucene session.
     */
    protected final FullTextSession getFullTextSession() {
        return fullTextSession;
    }
}
