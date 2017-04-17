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

package net.krotscheck.kangaroo.database.migration;

import com.mchange.v2.c3p0.PooledDataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.krotscheck.kangaroo.common.hibernate.lifecycle.SearchIndexContainerLifecycleListener;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;

/**
 * This lifecycle context listener ensures that the database is in the most
 * recent state.
 *
 * @author Michael Krotscheck
 */
public final class LiquibaseMigrationContainerListener
        implements ContainerLifecycleListener {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(SearchIndexContainerLifecycleListener.class);

    /**
     * The datasource.
     */
    private final PooledDataSource dataSource;

    /**
     * Create a new instance of the lifecycle listener, with an
     * injected factory.
     *
     * @param dataSource DataSource that provides our DB connections.
     */
    @Inject
    public LiquibaseMigrationContainerListener(
            final PooledDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Invoked at the {@link Container container} start-up. This method is
     * invoked even when application is reloaded and new instance of
     * application has started.
     *
     * @param container container that has been started.
     */
    @Override
    public void onStartup(final Container container) {
        try {
            JdbcConnection connection =
                    new JdbcConnection(dataSource.getConnection());

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(connection);

            Liquibase liquibase = new Liquibase(
                    "liquibase/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database);

            liquibase.update(new Contexts(), new LabelExpression());
        } catch (SQLException | LiquibaseException e) {
            logger.error("Cannot migrate database.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoked when the {@link Container container} has been reloaded.
     *
     * @param container container that has been reloaded.
     */
    @Override
    public void onReload(final Container container) {
        // Do nothing.
    }

    /**
     * Invoke at the {@link Container container} shut-down. This method is
     * invoked even before the application is being stopped as a part of reload.
     *
     * @param container container that has been shut down.
     */
    @Override
    public void onShutdown(final Container container) {
        // Do nothing.
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
