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
import net.krotscheck.kangaroo.test.rule.DatabaseResource;
import net.krotscheck.kangaroo.test.rule.database.ITestDatabase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.BadRequestException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Unit test for the liquibase migration listener.
 *
 * @author Michael Krotscheck
 */
public final class LiquibaseMigrationTest {

    /**
     * Reset the database factory (if necessary).
     */
    @After
    public void reset() {
        DatabaseFactory.reset();
    }

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
            PooledDataSource ps = mock(PooledDataSource.class);
            Connection testConnection = db.getConnection();
            Mockito.doReturn(testConnection).when(ps).getConnection();

            LiquibaseMigration listener = new LiquibaseMigration(ps);
            listener.get();

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
        PooledDataSource ps = mock(PooledDataSource.class);
        doThrow(LiquibaseException.class).when(ps).getConnection();

        LiquibaseMigration listener = new LiquibaseMigration(ps);

        try {
            listener.get();
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
        PooledDataSource ps = mock(PooledDataSource.class);
        doThrow(SQLException.class).when(ps).getConnection();

        LiquibaseMigration listener = new LiquibaseMigration(ps);

        try {
            listener.get();
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
        PooledDataSource ps = mock(PooledDataSource.class);
        Mockito.doReturn(null).when(ps).getConnection();

        LiquibaseMigration listener = new LiquibaseMigration(ps);

        try {
            listener.get();
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
    public void testExitAndCloseOnInternalException() throws Exception {
        // Create a database
        DatabaseResource resource = new DatabaseResource();
        try (ITestDatabase db = resource.createDatabase();
             Connection c = db.getConnection()) {
            Assert.assertFalse(hasTestTable(c));

            // Run the migration
            PooledDataSource ps = mock(PooledDataSource.class);
            Connection testConnection = db.getConnection();
            Mockito.doReturn(testConnection).when(ps).getConnection();

            DatabaseFactory mockFactory = mock(DatabaseFactory.class);
            doThrow(SQLException.class)
                    .when(mockFactory)
                    .findCorrectDatabaseImplementation(any());

            DatabaseFactory.setInstance(mockFactory);

            try {
                LiquibaseMigration listener = new LiquibaseMigration(ps);
                listener.get();
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
    public void testExitAndCloseOnUnexpectedException() throws Exception {
        // Create a database
        DatabaseResource resource = new DatabaseResource();
        try (ITestDatabase db = resource.createDatabase();
             Connection c = db.getConnection()) {
            Assert.assertFalse(hasTestTable(c));

            // Run the migration
            PooledDataSource ps = mock(PooledDataSource.class);
            Connection testConnection = db.getConnection();
            Mockito.doReturn(testConnection).when(ps).getConnection();

            DatabaseFactory mockFactory = mock(DatabaseFactory.class);
            doThrow(BadRequestException.class)
                    .when(mockFactory)
                    .findCorrectDatabaseImplementation(any());

            DatabaseFactory.setInstance(mockFactory);

            try {
                LiquibaseMigration listener = new LiquibaseMigration(ps);
                listener.get();
                Assert.fail();
            } catch (BadRequestException e) {
                Assert.assertNotNull(e);
            }

            // Assert that the connection is closed after migration.
            Assert.assertTrue(testConnection.isClosed());

            // Ensure that the tables have been created.
            Assert.assertFalse(hasTestTable(c));
        }
    }
}
