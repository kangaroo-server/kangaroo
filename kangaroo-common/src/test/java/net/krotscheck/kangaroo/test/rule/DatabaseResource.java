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

package net.krotscheck.kangaroo.test.rule;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.krotscheck.kangaroo.test.TestConfig;
import net.krotscheck.kangaroo.test.rule.database.ITestDatabase;
import net.krotscheck.kangaroo.test.rule.database.MariaDBTestDatabase;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.sql.Connection;
import java.util.TimeZone;

/**
 * This JUnit4 rule ensures that a fresh database has been created for this
 * particular test run, that a search index directory exists, and that the
 * database schema has been migrated into the test database.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseResource implements TestRule {

    static {
        // Force the database to use UTC.
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * The database database that'll provide us with test databases.
     */
    private ITestDatabase database;

    /**
     * Pull configuration settings from the system configuration.
     *
     * @return A database interface for the current database.
     */
    public ITestDatabase createDatabase() {
        switch (TestConfig.getDatabase()) {
            case MARIADB:
            default:
                database = new MariaDBTestDatabase();
        }
        database.create();
        return database;
    }

    /**
     * Return the JDBC Connection String for the database under test.
     *
     * @return The connection string.
     */
    public ITestDatabase getDatabase() {
        return database;
    }

    /**
     * Modifies the method-running {@link Statement} to implement this
     * test-running rule.
     *
     * @param base        The {@link Statement} to be modified
     * @param description A {@link Description} of the test implemented in
     *                    {@code base}
     * @return a new statement, which may be the same as {@code base},
     * a wrapper around {@code base}, or a completely new Statement.
     */
    @Override
    public Statement apply(final Statement base,
                           final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try (ITestDatabase db = createDatabase();
                     Connection c = db.getConnection()) {

                    // Create the liquibase bits.
                    JdbcConnection connection = new JdbcConnection(c);
                    Database database = DatabaseFactory.getInstance()
                            .findCorrectDatabaseImplementation(connection);
                    Liquibase liquibase =
                            new Liquibase(TestConfig.getDbChangelog(),
                                    new ClassLoaderResourceAccessor(),
                                    database);

                    // Migrate the database.
                    liquibase.update(new Contexts());

                    // Evaluate the next round of tests.
                    base.evaluate();

                    // Teardown the database.
                    liquibase.rollback(10000, "");
                }
            }
        };
    }
}
