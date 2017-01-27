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

/**
 * This class exposes all runtime parameters that may be used to configure
 * the test suites.
 *
 * @author Michael Krotscheck
 */
public final class TestConfig {

    /**
     * The JNDI path where the database may be accessed.
     *
     * @return A fully qualified JNDI Path.
     */
    public static String getDbJndiName() {
        return System.getProperty("test.db.jndiName", "OIDServerDB");
    }

    /**
     * Evaluate the Test Database JDBC Address.
     *
     * @return The test database address.
     */
    public static String getDbJdbcPath() {
        return System.getProperty("test.db.jdbc",
                "jdbc:h2:mem:target/test/db/h2/hibernate");
    }

    /**
     * Evaluate the Test Database Driver.
     *
     * @return The test database driver class as a string.
     */
    public static String getDbDriver() {
        return System.getProperty("test.db.driver", "org.h2.Driver");
    }

    /**
     * Evaluate the Test Database Login.
     *
     * @return The test database login.
     */
    public static String getDbLogin() {
        return System.getProperty("test.db.user", "oid");
    }

    /**
     * Evaluate the Test Database Password.
     *
     * @return The test database password.
     */
    public static String getDbPassword() {
        return System.getProperty("test.db.password", "oid");
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

    /**
     * Utility class, private constructor.
     */
    private TestConfig() {

    }
}
