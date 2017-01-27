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

import net.krotscheck.kangaroo.test.TestConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * An abstract class that makes available various database resources,
 * including connection construction.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractDBRule implements TestRule {

    static {
        // Force the database to use UTC.
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * The single pooled data source from which all connections in the test
     * suite are sourced.
     */
    private static ThreadLocal<BasicDataSource> dataSource
            = new ThreadLocal<>();

    /**
     * The number of DS invocations there've been so far.
     */
    private static Integer dsCounter = 0;

    /**
     * Return the current datasource.
     *
     * @return The datasource.
     */
    protected BasicDataSource getDataSource() {
        return dataSource.get();
    }

    /**
     * Initialize the datasource before.
     */
    private void initializeDatasource() {
        if (dataSource.get() == null) {
            BasicDataSource newSource = new BasicDataSource();
            newSource.setDriverClassName(TestConfig.getDbDriver());
            newSource.setUrl(TestConfig.getDbJdbcPath());
            newSource.setUsername(TestConfig.getDbLogin());
            newSource.setPassword(TestConfig.getDbPassword());
            newSource.setMaxIdle(1);
            dataSource.set(newSource);
        }
        dsCounter++;
    }

    /**
     * Destroy the existing datasource.
     *
     * @throws SQLException If we can't close the connection.
     */
    private void destroyDatasource() throws SQLException {
        dsCounter--;
        if (dsCounter == 0) {
            BasicDataSource oldSource = dataSource.get();
            if (!oldSource.isClosed()) {
                oldSource.close();
            }
            dataSource.remove();
        }
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
    public final Statement apply(final Statement base,
                                 final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                initializeDatasource();
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                    destroyDatasource();
                }
            }
        };
    }

    /**
     * Execute a query against the current database, and return the results.
     *
     * @param query The query to execute.
     * @return A map of the results.
     * @throws SQLException Thrown if the query borks.
     */
    protected final List<Map<String, String>> executeQuery(final String query)
            throws SQLException {

        try (
                Connection conn = getDataSource().getConnection();
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
        }
    }

    /**
     * Actions to perform before the test.
     *
     * @throws Throwable Exceptions thrown during setup.
     */
    protected abstract void before() throws Throwable;

    /**
     * Actions to perform after the test.
     *
     * @throws Throwable Exceptions thrown during teardown.
     */
    protected abstract void after() throws Throwable;
}
