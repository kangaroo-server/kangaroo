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

package net.krotscheck.kangaroo.common.hibernate.factory;

import com.mchange.v2.c3p0.PooledDataSource;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Supplier;

/**
 * This factory extracts the pooled connection that underlies Hibernate's
 * session factory. Its goal is to simplify accessing the database directly,
 * though I do not recommend its use.
 *
 * @author Michael Krotscheck
 */
public final class PooledDataSourceFactory
        implements Supplier<PooledDataSource> {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(PooledDataSourceFactory.class);
    /**
     * Session factory for this environment.
     */
    private final SessionFactoryImpl sessionFactory;

    /**
     * Create a new pooled data source factory.
     *
     * @param sessionFactory The Hibernate Session Factory, Injected.
     */
    @Inject
    public PooledDataSourceFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory.unwrap(SessionFactoryImpl.class);
    }

    /**
     * Provide a singleton instance of the hibernate session factory.
     *
     * @return A session factory.
     */
    @Override
    public PooledDataSource get() {
        logger.trace("Extracting PooledDataSource from hibernate session "
                + "factory.");
        SessionFactoryServiceRegistryImpl src =
                (SessionFactoryServiceRegistryImpl)
                        sessionFactory.getServiceRegistry();
        ConnectionProvider cp = src.getService(ConnectionProvider.class);
        return cp.unwrap(PooledDataSource.class);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(PooledDataSourceFactory.class)
                    .to(PooledDataSource.class)
                    .in(Singleton.class);
        }
    }
}
