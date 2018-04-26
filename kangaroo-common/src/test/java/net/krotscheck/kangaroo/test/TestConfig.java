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
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.hibernate.dialect.H2Dialect;

import java.io.File;

/**
 * This class exposes all runtime parameters that may be used to configure
 * the test suites.
 *
 * @author Michael Krotscheck
 */
public final class TestConfig {

    /**
     * Utility class, private constructor.
     */
    private TestConfig() {

    }

    /**
     * Get the database type under test.
     *
     * @return The path.w
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
     * Evaluate the MariaDB Root Database User (Where appropriate).
     *
     * @return The root database user.
     */
    public static String getMariaDBRootUser() {
        // The root password can exist in several locations.
        String home = System.getProperty("user.home");
        File f = new File(home + "/.my.cnf");

        // If there's a .my.cnf to access, use that.
        try {
            HierarchicalINIConfiguration configuration =
                    new HierarchicalINIConfiguration(f);
            return configuration
                    .getSection("client")
                    .getString("user");
        } catch (ConfigurationException e) {
            // Fallback to system properties.
            return System.getProperty("hibernate.root.user", "root");
        }
    }

    /**
     * Evaluate the MariaDB Root Database Password (Where appropriate).
     *
     * @return The root database password.
     */
    public static String getMariaDBRootPassword() {
        // The root password can exist in several locations.
        String home = System.getProperty("user.home");
        File f = new File(home + "/.my.cnf");

        // If there's a .my.cnf to access, use that.
        try {
            HierarchicalINIConfiguration configuration =
                    new HierarchicalINIConfiguration(f);
            return configuration
                    .getSection("client")
                    .getString("password");
        } catch (ConfigurationException e) {
            // Fallback to system properties.
            return System.getProperty("hibernate.root.password", "");
        }
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

    /**
     * Evaluate the facebook test app id. Must be set.
     *
     * @return The facebook app id.
     */
    public static String getFacebookAppId() {
        return System.getenv("KANGAROO_FB_APP_USR");
    }

    /**
     * Evaluate the facebook test app secret. Must be set.
     *
     * @return The facebook app secret.
     */
    public static String getFacebookAppSecret() {
        return System.getenv("KANGAROO_FB_APP_PSW");
    }

    /**
     * Evaluate the github test app id. Must be set.
     *
     * @return The github app id.
     */
    public static String getGithubAppId() {
        return System.getenv("KANGAROO_GITHUB_APP_USR");
    }

    /**
     * Evaluate the github test app secret. Must be set.
     *
     * @return The github app secret.
     */
    public static String getGithubAppSecret() {
        return System.getenv("KANGAROO_GITHUB_APP_PSW");
    }

    /**
     * Evaluate the github test user login. Must be set.
     *
     * @return The github test user login.
     */
    public static String getGithubAccountId() {
        return System.getenv("KANGAROO_GITHUB_ACCOUNT_USR").trim();
    }

    /**
     * Evaluate the github test user password. Must be set.
     *
     * @return The github test user password.
     */
    public static String getGithubAccountSecret() {
        return System.getenv("KANGAROO_GITHUB_ACCOUNT_PSW").trim();
    }

    /**
     * Evaluate the google test app id. Must be set.
     *
     * @return The google app id.
     */
    public static String getGoogleAppId() {
        return System.getenv("KANGAROO_GOOGLE_APP_USR");
    }

    /**
     * Evaluate the google test app secret. Must be set.
     *
     * @return The google app secret.
     */
    public static String getGoogleAppSecret() {
        return System.getenv("KANGAROO_GOOGLE_APP_PSW");
    }

    /**
     * Evaluate the google test app id. Must be set.
     *
     * @return The google app id.
     */
    public static String getGoogleAccountId() {
        return System.getenv("KANGAROO_GOOGLE_ACCOUNT_USR");
    }

    /**
     * Evaluate the google test app secret. Must be set.
     *
     * @return The google app secret.
     */
    public static String getGoogleAccountSecret() {
        return System.getenv("KANGAROO_GOOGLE_ACCOUNT_PSW");
    }

    /**
     * Get a static testing port. This is used to spin up a jersey test
     * instance for tests that require a remotely registered address; such as
     * an OAuth2 registered redirect.
     *
     * @return A port which should be unique _per test run_.
     */
    public static String getTestingPort() {
        return "7777";
    }
}
