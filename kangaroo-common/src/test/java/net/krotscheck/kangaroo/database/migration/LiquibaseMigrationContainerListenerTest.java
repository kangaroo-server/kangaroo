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
import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import net.krotscheck.kangaroo.test.rule.database.ITestDatabase;
import org.glassfish.jersey.server.spi.Container;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unit test for the liquibase migration listener.
 *
 * @author Michael Krotscheck
 */
public final class LiquibaseMigrationContainerListenerTest {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(LiquibaseMigrationContainerListenerTest.class);

    /**
     * Check to make sure the applications table does not exist. Since some
     * databases include their own meta tables (h2 for instance), we're
     * checking for the existence of a table we create, rather than one
     * provided by the database.s
     *
     * @param c A connection instance to the database.
     * @return True if the 'applications' table exists.
     * @throws Exception Should not be thrown.
     */
    private boolean hasApplicationTable(final Connection c) throws Exception {
        DatabaseMetaData m = c.getMetaData();
        ResultSet rs = m.getTables(c.getCatalog(), null, "%",
                null);

        List<String> tables = new ArrayList<>();
        int total = 0;
        while (rs.next()) {
            tables.add(rs.getString("TABLE_NAME"));
        }
        rs.close();

        // lowercase to normalize.
        tables = tables.stream()
                .map(String::toLowerCase)
                .filter("applications"::equals)
                .collect(Collectors.toList());

        return tables.size() > 0;
    }

    /**
     * Make sure the database is migrated on startup. We're only testing the
     * liquibase interface here, not an actual full migration.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testMigrateOnStartup() throws Exception {
        // Create a database
        DatabaseResource resource = new DatabaseResource();
        try (ITestDatabase db = resource.createDatabase();
             Connection c = db.getConnection()) {
            Assert.assertFalse(hasApplicationTable(c));

            // Run the migration
            PooledDataSource ps = Mockito.mock(PooledDataSource.class);
            Container container = Mockito.mock(Container.class);
            Mockito.doReturn(c).when(ps).getConnection();

            LiquibaseMigrationContainerListener listener =
                    new LiquibaseMigrationContainerListener(ps);
            listener.onStartup(container);

            // Ensure that the tables have been created.
            Assert.assertTrue(hasApplicationTable(c));
        }
    }

    /**
     * Assert that the program exit if there's a migration exception.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testExitOnMigrationFailure() throws Exception {
        // Throw an exception via a fake pooled datasource
        PooledDataSource ps = Mockito.mock(PooledDataSource.class);
        Mockito.doThrow(SQLException.class).when(ps).getConnection();
        Container c = Mockito.mock(Container.class);

        LiquibaseMigrationContainerListener listener =
                new LiquibaseMigrationContainerListener(ps);

        try {
            listener.onStartup(c);
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Make sure nothing happens on shutdown.
     */
    @Test
    public void testNoShutDownInteraction() {
        Container c = Mockito.mock(Container.class);
        PooledDataSource ps = Mockito.mock(PooledDataSource.class);
        LiquibaseMigrationContainerListener listener =
                new LiquibaseMigrationContainerListener(ps);

        listener.onShutdown(c);
        Mockito.verifyNoMoreInteractions(c);
    }

    /**
     * Make sure nothing happens on restart.
     */
    @Test
    public void testNoReloadInteraction() {
        Container c = Mockito.mock(Container.class);
        PooledDataSource ps = Mockito.mock(PooledDataSource.class);
        LiquibaseMigrationContainerListener listener =
                new LiquibaseMigrationContainerListener(ps);

        listener.onReload(c);
        Mockito.verifyNoMoreInteractions(c);
    }
}
