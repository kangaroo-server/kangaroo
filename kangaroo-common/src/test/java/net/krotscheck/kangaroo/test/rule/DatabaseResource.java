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
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.TimeZone;

/**
 * This JUnit4 rule ensures that the JNDI resource has been bootstrapped, and
 * that the database schema has been migrated into the test database.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseResource implements TestRule {

    /**
     * JDBC Connection string.
     */
//    public static final String JDBC =
//            "jdbc:mysql://localhost:3306/oid?useUnicode=yes";
    public static final String JDBC =
            "jdbc:h2:mem:target/test/db/h2/hibernate";

    /**
     * JDBC Connection string.
     */
//    public static final String DRIVER = "com.mysql.jdbc.Driver";
    public static final String DRIVER = "org.h2.Driver";

    /**
     * JDBC Connection string.
     */
    public static final String USER = "oid";

    /**
     * JDBC Connection string.
     */
    public static final String PASSWORD = "oid";

    /**
     * The JNDI Identity.
     */
    private static final String JNDI = "java:/comp/env/jdbc/OIDServerDB";

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(DatabaseResource.class);

    /**
     * Database connection, currently active.
     */
    private Connection conn;

    /**
     * The currently active liquibase context.
     */
    private Liquibase liquibase;

    /**
     * Set up a JDNI connection for your tests.
     */
    public void setupJNDI() {
        logger.debug("Setting up JNDI.");
        // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.eclipse.jetty.jndi.InitialContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES,
                "org.eclipse.jetty.jndi");

        try {
            InitialContext ic = new InitialContext();

            ic.createSubcontext("java:/comp/env");
            ic.createSubcontext("java:/comp/env/jdbc");

            BasicDataSource bds = new BasicDataSource();
            bds.setDriverClassName(
                    System.getProperty("test.db.driver", DRIVER));
            bds.setUrl(System.getProperty("test.db.jdbc", JDBC));
            bds.setUsername(System.getProperty("test.db.user", USER));
            bds.setPassword(System.getProperty("test.db.password", PASSWORD));
            ic.bind(JNDI, bds);
        } catch (NamingException e) {
            // Do nothing, this is only thrown if the context already exists,
            // which will happen in a second test run.
            logger.debug("JDNI Naming Exception, resource likely already "
                    + "exists.");
        }
    }

    /**
     * Migrate the current database using the existing liquibase schema.
     *
     * @throws Exception Exceptions thrown during migration. If these fail,
     *                   fix your tests!
     */
    public void setupDatabase() throws Exception {
        logger.info("Migrating Database Schema.");

        // Force the database to use UTC.
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Class.forName(System.getProperty("test.db.driver", DRIVER));
        conn = DriverManager.getConnection(
                System.getProperty("test.db.jdbc", JDBC),
                System.getProperty("test.db.user", USER),
                System.getProperty("test.db.password", PASSWORD));

        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(conn));

        liquibase = new Liquibase("liquibase/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts());
    }

    /**
     * Clean any previously established database schema.
     *
     * @throws Throwable An exception encountered during teardown.
     */
    public void cleanDatabase() throws Throwable {
        logger.info("Cleaning Database.");
        liquibase.rollback(1000, null);
        liquibase = null;

        logger.debug("Closing connection.");
        conn.close();
        conn = null;
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
                setupJNDI();
                setupDatabase();
                try {
                    base.evaluate();
                } finally {
                    cleanDatabase();
                }
            }
        };
    }
}
