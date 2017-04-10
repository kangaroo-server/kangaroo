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

import org.hibernate.dialect.Dialect;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * This interface describes a class by which a test database may be
 * provisioned. It is assumed that the returned DBReference instance will
 * maintain the lifecycle of the database (as perhaps with short-lived h2
 * databases).
 */
public interface ITestDatabase extends AutoCloseable {

    /**
     * Create a new test database.
     *
     * @return Reference to this database (which is autocloseable).
     */
    ITestDatabase create();

    /**
     * Retrieve a database connection from the current active test database.
     *
     * @return A connection instance to the database. Close this manually.
     */
    Connection getConnection();

    /**
     * Load the driver for this database.
     */
    void loadDriver();

    /**
     * Get the JDBC Connection String for this test database.
     *
     * @return The connection string.
     */
    String getJdbcConnectionString();

    /**
     * The name for the created database.
     *
     * @return The username.
     */
    String getName();

    /**
     * The user for the created database.
     *
     * @return The username.
     */
    String getUser();

    /**
     * The password to the created database.
     *
     * @return The password.
     */
    String getPassword();

    /**
     * Return the dialect for this database, in string format.
     *
     * @return Dialect class, in string format.
     */
    String getDialect();

    /**
     * Return the class name for the database driver class, in string format.
     *
     * @return Driver class, in string format.
     */
    String getDriverClass();
}
