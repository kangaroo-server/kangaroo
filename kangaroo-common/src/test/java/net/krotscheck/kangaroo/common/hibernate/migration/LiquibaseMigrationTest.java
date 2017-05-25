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

package net.krotscheck.kangaroo.common.hibernate.migration;

import com.mchange.v2.c3p0.PooledDataSource;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidClientException;
import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import net.krotscheck.kangaroo.test.rule.database.ITestDatabase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
@RunWith(PowerMockRunner.class)
public final class LiquibaseMigrationTest {

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
    private boolean hasTestTable(final Connection c) throws Exception {
        DatabaseMetaData m = c.getMetaData();
        ResultSet rs = m.getTables(c.getCatalog(), null, "%",
                null);

        List<String> tables = new ArrayList<>();
        while (rs.next()) {
            tables.add(rs.getString("TABLE_NAME"));
        }
        rs.close();

        // lowercase to normalize.
        tables = tables.stream()
                .map(String::toLowerCase)
                .filter("test"::equals)
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
            Assert.assertFalse(hasTestTable(c));

            // Run the migration
            PooledDataSource ps = Mockito.mock(PooledDataSource.class);
            Connection testConnection = db.getConnection();
            Mockito.doReturn(testConnection).when(ps).getConnection();

            LiquibaseMigration listener = new LiquibaseMigration(ps);
            listener.provide();

            // Assert that the connection is closed after migration.
            Assert.assertTrue(testConnection.isClosed());

            // Ensure that the tables have been created.
            Assert.assertTrue(hasTestTable(c));
        }
    }

    /**
     * Assert that the program exit if there's a migration exception.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testExitOnMigrationFailureLiquibaseException()
            throws Exception {
        // Throw an exception via a fake pooled datasource
        PooledDataSource ps = Mockito.mock(PooledDataSource.class);
        Mockito.doThrow(LiquibaseException.class).when(ps).getConnection();

        LiquibaseMigration listener = new LiquibaseMigration(ps);

        try {
            listener.provide();
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Assert that the program exit if there's a migration exception.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testExitOnMigrationFailureSQLException() throws Exception {
        // Throw an exception via a fake pooled datasource
        PooledDataSource ps = Mockito.mock(PooledDataSource.class);
        Mockito.doThrow(SQLException.class).when(ps).getConnection();

        LiquibaseMigration listener = new LiquibaseMigration(ps);

        try {
            listener.provide();
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Assert that the program exits if getConnection() is null.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testExitOnMigrationFailureNullConnection() throws Exception {
        // Throw an exception via a fake pooled datasource
        PooledDataSource ps = Mockito.mock(PooledDataSource.class);
        Mockito.doReturn(null).when(ps).getConnection();

        LiquibaseMigration listener = new LiquibaseMigration(ps);

        try {
            listener.provide();
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Assert that the program exits if an expected exception is thrown.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    @PrepareOnlyThisForTest(DatabaseFactory.class)
    public void testExitAndCloseOnInternalException() throws Exception {
        // mock all the static methods in a class called "Static"
        PowerMockito.mockStatic(DatabaseFactory.class);

        // Create a database
        DatabaseResource resource = new DatabaseResource();
        try (ITestDatabase db = resource.createDatabase();
             Connection c = db.getConnection()) {
            Assert.assertFalse(hasTestTable(c));

            // Run the migration
            PooledDataSource ps = Mockito.mock(PooledDataSource.class);
            Connection testConnection = db.getConnection();
            Mockito.doReturn(testConnection).when(ps).getConnection();

            Mockito.when(DatabaseFactory.getInstance())
                    .thenThrow(SQLException.class);

            try {
                LiquibaseMigration listener = new LiquibaseMigration(ps);
                listener.provide();
                Assert.fail();
            } catch (RuntimeException e) {
                Assert.assertNotNull(e);
            }

            // Assert that the connection is closed after migration.
            Assert.assertTrue(testConnection.isClosed());

            // Ensure that the tables have been created.
            Assert.assertFalse(hasTestTable(c));
        }
    }

    /**
     * Assert that the program exits if an unexpected exception is thrown.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    @PrepareOnlyThisForTest(DatabaseFactory.class)
    public void testExitAndCloseOnUnexpectedException() throws Exception {
        // mock all the static methods in a class called "Static"
        PowerMockito.mockStatic(DatabaseFactory.class);

        // Create a database
        DatabaseResource resource = new DatabaseResource();
        try (ITestDatabase db = resource.createDatabase();
             Connection c = db.getConnection()) {
            Assert.assertFalse(hasTestTable(c));

            // Run the migration
            PooledDataSource ps = Mockito.mock(PooledDataSource.class);
            Connection testConnection = db.getConnection();
            Mockito.doReturn(testConnection).when(ps).getConnection();

            Mockito.when(DatabaseFactory.getInstance())
                    .thenThrow(InvalidClientException.class);

            try {
                LiquibaseMigration listener = new LiquibaseMigration(ps);
                listener.provide();
                Assert.fail();
            } catch (InvalidClientException e) {
                Assert.assertNotNull(e);
            }

            // Assert that the connection is closed after migration.
            Assert.assertTrue(testConnection.isClosed());

            // Ensure that the tables have been created.
            Assert.assertFalse(hasTestTable(c));
        }
    }

    /**
     * Make sure nothing happens on dispose.
     */
    @Test
    @PrepareOnlyThisForTest(DatabaseMigrationState.class)
    public void testNoShutDownInteraction() {
        DatabaseMigrationState marker =
                PowerMockito.mock(DatabaseMigrationState.class);
        PooledDataSource ps = Mockito.mock(PooledDataSource.class);
        LiquibaseMigration listener = new LiquibaseMigration(ps);

        listener.dispose(marker);
        Mockito.verifyNoMoreInteractions(marker);
    }
}
