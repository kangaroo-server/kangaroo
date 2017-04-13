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

import net.krotscheck.kangaroo.test.TestConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Common functionality shared between test databases.
 *
 * @author Michael Krotscheckz
 */
public abstract class AbstractTestDatabase implements ITestDatabase {

    /**
     * The user for the created database.
     *
     * @return The username.
     */
    @Override
    public final String getUser() {
        return TestConfig.getDbLogin();
    }

    /**
     * The password to the created database.
     *
     * @return The password.
     */
    @Override
    public final String getPassword() {
        return TestConfig.getDbPassword();
    }

    /**
     * Return the JDBC Connection String for this database.
     *
     * @return The connection string.
     */
    @Override
    public String getJdbcConnectionString() {
        return TestConfig.getDbJdbcPath();
    }

    /**
     * Return the dialect for this database, in string format.
     *
     * @return Dialect class, in string format.
     */
    @Override
    public String getDialect() {
        return TestConfig.getDbDialect();
    }

    /**
     * Return the class name for the database driver class, in string format.
     *
     * @return Driver class, in string format.
     */
    @Override
    public String getDriverClass() {
        return TestConfig.getDbDriver();
    }


    /**
     * Execute a query against the current database, and return the results.
     *
     * @param query The query to execute.
     * @return A map of the results.
     */
    protected List<Map<String, String>> executeQuery(final String query) {
        try (
                Connection conn = getConnection();
                java.sql.Statement stmt = conn.createStatement()
        ) {
            ResultSet rs = stmt.executeQuery(query);

            List<Map<String, String>> results = new ArrayList<>();

            ResultSetMetaData metaData = rs.getMetaData();
            List<String> columnNames = new ArrayList<>();
            for (Integer i = 1; i <= metaData.getColumnCount(); i++) {
                columnNames.add(metaData.getColumnLabel(i));
            }

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                columnNames.forEach((name) -> {
                    try {
                        row.put(name, rs.getString(name));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
                results.add(row);
            }
            rs.close();
            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load the driver for this database.
     */
    @Override
    public final void loadDriver() {
        try {
            Class.forName(getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load DB Driver", e);
        }
    }
}
