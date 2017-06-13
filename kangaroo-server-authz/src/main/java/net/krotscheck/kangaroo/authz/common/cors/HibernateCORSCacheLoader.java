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

package net.krotscheck.kangaroo.authz.common.cors;

import com.google.common.cache.CacheLoader;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientReferrer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;

/**
 * This validator returns true if the CORS domain exists in our referrer
 * table. Results are cached for 5 minutes, which is our CORS cache timeout.
 *
 * @author Michael Krotscheck
 */
public final class HibernateCORSCacheLoader
        extends CacheLoader<URI, Boolean> {

    /**
     * The hibernate session factory.
     */
    private final SessionFactory factory;

    /**
     * The hibernate CORS session factory.
     *
     * @param factory Injected hibernate factory.
     */
    @Inject
    public HibernateCORSCacheLoader(final SessionFactory factory) {
        this.factory = factory;
    }

    /**
     * Computes or retrieves the value corresponding to {@code key}.
     *
     * @param origin The non-null key whose value should be loaded.
     * @return Whether the URI is valid or not.
     * @throws Exception If unable to load the result.
     */
    @Override
    public Boolean load(final URI origin) throws Exception {
        Session s = factory.openSession();
        try {
            Transaction t = s.beginTransaction();
            Criteria c = s.createCriteria(ClientReferrer.class)
                    .add(Restrictions.eq("uri", origin))
                    .setProjection(Projections.rowCount());
            Long total = Long.valueOf(c.uniqueResult().toString());
            t.commit();
            return total > 0;
        } catch (HibernateException e) {
            // Do nothing.
            return false;
        } finally {
            s.close();
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(HibernateCORSCacheLoader.class)
                    .to(HibernateCORSCacheLoader.class)
                    .in(Singleton.class);
        }
    }
}
