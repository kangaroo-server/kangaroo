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

package net.krotscheck.kangaroo.test;

import net.krotscheck.kangaroo.test.rule.database.TestDB;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exposes all runtime parameters that may be used to configure
 * the test suites.
 *
 * @author Michael Krotscheck
 */
public final class TestConfig {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory.getLogger(TestConfig.class);

    /**
     * Utility class, private constructor.
     */
    private TestConfig() {

    }

    /**
     * Get the database type under test.
     *
     * @return The path.
     */
    public static TestDB getDatabase() {
        return TestDB.fromDialect(getDbDialect());
    }

    /**
     * Evaluate the Test Database JDBC Address.
     *
     * @return The test database address.
     */
    public static String getDbJdbcPath() {
        return System.getProperty("hibernate.connection.url",
                "jdbc:h2:file:./target/h2.test.db");
    }

    /**
     * Evaluate the Test Database Driver.
     *
     * @return The test database driver class as a string.
     */
    public static String getDbDriver() {
        return System.getProperty("hibernate.connection.driver_class",
                "org.h2.Driver");
    }

    /**
     * Evaluate the Test Database Login.
     *
     * @return The test database login.
     */
    public static String getDbLogin() {
        return System.getProperty("hibernate.connection.username", "oid");
    }

    /**
     * Evaluate the Test Database Password.
     *
     * @return The test database password.
     */
    public static String getDbPassword() {
        return System.getProperty("hibernate.connection.password", "oid");
    }

    /**
     * Evaluate the Root Database Password (Where appropriate).
     *
     * @return The root database password.
     */
    public static String getRootPassword() {
        return System.getProperty("hibernate.root.password", "");
    }

    /**
     * Evaluate the Test Database Password.
     *
     * @return The test database password.
     */
    public static String getDbDialect() {
        return System.getProperty("hibernate.dialect",
                H2Dialect.class.getSimpleName());
    }

    /**
     * Evaluate the test database changelog.
     *
     * @return Resource path to the database changelog.
     */
    public static String getDbChangelog() {
        return System.getProperty("test.db.changelog",
                "liquibase/db.changelog-master.yaml");
    }
}
