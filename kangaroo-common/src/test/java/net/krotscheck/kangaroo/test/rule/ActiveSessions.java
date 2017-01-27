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

package net.krotscheck.kangaroo.test.rule;

import net.krotscheck.kangaroo.test.TestConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This JUnit4 rule provides a few handy methods that allow one to mark(),
 * and then verify(), the current number of open connections against the
 * database.
 *
 * @author Michael Krotscheck
 */
public final class ActiveSessions implements TestRule {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(DatabaseResource.class);

    /**
     * THe marked list of initial sessions.
     */
    private List<Map<String, String>> initialSessions = new ArrayList<>();

    /**
     * Mark the current number of active sessions.
     */
    public void mark() {
        initialSessions = getActiveSessions();
    }

    /**
     * Check that the previously marked number of active sessions has not
     * been modified.
     *
     * @return Whether there are extra, lingering sessions.
     */
    public Boolean check() {
        List<Map<String, String>> lingeringSessions = getActiveSessions()
                .stream()
                .filter(row -> !initialSessions.contains(row))
                .collect(Collectors.toList());
        if (lingeringSessions.size() > 0) {
            String message = String.format("Database sessions did not clean "
                    + "up after themselves. %s extra sessions "
                    + "detected.", lingeringSessions.size());
            // Log out the problems.
            logger.error(message);
            lingeringSessions.forEach((item) -> logger.error(item.toString()));
            return true;
        }
        return false;
    }

    /**
     * Retrieve the current number of active sessions.
     *
     * @return The number of active connections.
     */
    private List<Map<String, String>> getActiveSessions() {
        try {
            switch (TestConfig.getDbDriver()) {
                case "com.mysql.jdbc.Driver":
                    return new ArrayList<>();
                case "org.h2.Driver":
                default:
                    String query = "select ID, STATEMENT, SESSION_START, "
                            + "USER_NAME, CONTAINS_UNCOMMITTED "
                            + "from information_schema.sessions";
                    List<Map<String, String>> results = executeQuery(query)
                            .stream()
                            .filter(row -> !query.equals(row.get("STATEMENT")))
                            .collect(Collectors.toList());
                    return results;
            }
        } catch (Throwable sqle) {
            // Don't do this. Fix it.
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Execute a query against the current database, and return the results.
     *
     * @param query The query to execute.
     * @return A map of the results.
     * @throws Throwable Thrown if the query borks.
     */
    private List<Map<String, String>> executeQuery(final String query)
            throws Throwable {
        InitialContext ctx = new InitialContext();
        BasicDataSource dataSource =
                (BasicDataSource) ctx.lookup(TestConfig.getDbJndiPath());

        try (
                Connection conn = dataSource.getConnection();
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
                base.evaluate();
            }
        };
    }
}
