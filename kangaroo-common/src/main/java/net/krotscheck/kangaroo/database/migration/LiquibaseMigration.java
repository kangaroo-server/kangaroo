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
import liquibase.changelog.ChangeSetStatus;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.krotscheck.kangaroo.common.hibernate.lifecycle.SearchIndexContainerLifecycleListener;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * This factory runs the database migration.
 *
 * @author Michael Krotscheck
 */
public final class LiquibaseMigration
        implements Factory<DatabaseMigrationState> {

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
    public LiquibaseMigration(final PooledDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * This method will migrate the database, keep track of every changeset
     * that is applied, collecting the results.
     *
     * @return The produces object
     */
    @Override
    public DatabaseMigrationState provide() {
        try {
            Connection c = dataSource.getConnection();

            try {
                JdbcConnection connection = new JdbcConnection(c);

                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(connection);

                Liquibase liquibase = new Liquibase(
                        "liquibase/db.changelog-master.yaml",
                        new ClassLoaderResourceAccessor(),
                        database);

                // Determine the list of run changesets
                List<ChangeSetStatus> changesets = liquibase
                        .getChangeSetStatuses(new Contexts(),
                                new LabelExpression());

                LiquibaseMigrationWatcher w =
                        new LiquibaseMigrationWatcher(changesets);

                liquibase.setChangeExecListener(w);
                liquibase.update(new Contexts(), new LabelExpression());

                return new DatabaseMigrationState(w.isMigrated(),
                        w.getCurrentVersion());
            } finally {
                c.close();
            }
        } catch (SQLException | LiquibaseException e) {
            logger.error("Cannot migrate database.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Do nothing, migration is a run-once operation.
     *
     * @param instance The instance to dispose of
     */
    @Override
    public void dispose(final DatabaseMigrationState instance) {

    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(LiquibaseMigration.class)
                    .to(DatabaseMigrationState.class)
                    .in(Singleton.class);
        }
    }
}
