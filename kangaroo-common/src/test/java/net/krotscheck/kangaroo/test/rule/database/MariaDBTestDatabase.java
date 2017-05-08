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

package net.krotscheck.kangaroo.test.rule.database;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Create a new test database using the mariadb driver. We assume that the
 * database is available on 127.0.0.1:3306, and that the root password is
 * blank. If this is not the case, your testing environment is likely not
 * ephemeral enough to be secure.
 *
 * The reason we're using MariaDB instead of MySQL is because the mysql
 * driver is GPL'd. Tough cookies, Oracle.
 *
 * @author Michael Krotscheck
 */
public final class MariaDBTestDatabase extends AbstractTestDatabase
        implements ITestDatabase {

    /**
     * The root password for this database.
     */
    private final String rootPassword;

    /**
     * Create a new DB reference.
     *
     * @param rootPassword The root password for the database.
     */
    public MariaDBTestDatabase(final String rootPassword) {
        this.rootPassword = rootPassword;
    }

    /**
     * The name for the created database.
     *
     * @return The username.
     */
    @Override
    public String getName() {
        String url = getJdbcConnectionString();
        url = url.substring("jdbc:".length());
        URI path = UriBuilder.fromUri(url)
                .replaceQuery("")
                .build();
        return path.getPath().substring("/".length());
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     *
     * @throws IOException if this resource cannot be closed
     */
    @Override
    public void close() throws IOException {
        try (Connection connection = createRootConnection()) {
            String query = String.format("DROP DATABASE %s;", getName());
            String permQuery = String.format("REVOKE ALL ON %s.* FROM"
                    + " %s@localhost;", getName(), getUser());

            try (Statement stmt = connection.createStatement()) {
                stmt.addBatch(query);
                stmt.addBatch(permQuery);
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException("cannot delete maria database", e);
        }
    }

    /**
     * Create a new test database.
     *
     * @return Reference to this database (which is autocloseable).
     */
    @Override
    public ITestDatabase create() {
        try (Connection connection = createRootConnection()) {
            String query = String
                    .format("CREATE DATABASE IF NOT EXISTS %s;", getName());
            String permQuery = String.format("GRANT ALL ON %s.* TO"
                            + " %s@localhost IDENTIFIED BY '%s';", getName(),
                    getUser(), getPassword());

            try (Statement stmt = connection.createStatement()) {
                stmt.addBatch(query);
                stmt.addBatch(permQuery);
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException("cannot create maria database", e);
        }

        return this;
    }

    /**
     * Get an autocloseable instance of a database connection. You should
     * close this yourself.
     *
     * @return The connection string.
     */
    @Override
    public Connection getConnection() {
        loadDriver();
        try {
            return DriverManager.getConnection(getJdbcConnectionString(),
                    getUser(),
                    getPassword());
        } catch (SQLException e) {
            throw new RuntimeException("cannot connect to maria database", e);
        }
    }

    /**
     * Build a connection instance for the root user of the database.
     *
     * @return A connection to the database with root credentials.
     */
    private Connection createRootConnection() {
        loadDriver();
        try {
            // Build a connection string without the database.
            String dbUrl = getJdbcConnectionString();
            dbUrl = dbUrl.substring("jdbc:".length());
            URI dbpath = UriBuilder.fromPath(dbUrl).path("").build();
            String rootJdbc = String.format("jdbc:%s://%s:%s/",
                    dbpath.getScheme(),
                    dbpath.getHost(),
                    dbpath.getPort());

            return DriverManager.getConnection(rootJdbc, "root",
                    rootPassword);
        } catch (SQLException e) {
            throw new RuntimeException("cannot connect to maria database",
                    e);
        }
    }
}
