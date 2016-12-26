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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This JUnit4 rule ensures that the JNDI resource has been bootstrapped, and
 * that the database schema has been migrated into the test database.
 *
 * @author Michael Krotscheck
 */
public final class ActiveSessions extends AbstractDBRule {

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
            switch (getDbDriver()) {
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
        } catch (SQLException sqle) {
            // Don't do this. Fix it.
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Actions to perform before the test.
     *
     * @throws Throwable Exceptions thrown during setup.
     */
    @Override
    protected void before() throws Throwable {

    }

    /**
     * Actions to perform after the test.
     *
     * @throws Throwable Exceptions thrown during teardown.
     */
    @Override
    protected void after() throws Throwable {

    }
}
