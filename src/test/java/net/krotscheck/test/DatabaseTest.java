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
 */

package net.krotscheck.test;

import org.glassfish.jersey.test.JerseyTest;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;

/**
 * A test suite that sets up a database for the services to run against.
 *
 * @author Michael Krotscheck
 */
public abstract class DatabaseTest extends JerseyTest {

    /**
     * JDBC Connection string.
     */
    private static final String JDBC =
            "jdbc:h2:mem:target/test/db/h2/hibernate";

    /**
     * The JNDI connection for the test.
     */
    private static Connection conn;

    /**
     * The liquibase instance that handles our migration.
     */
    private static Liquibase liquibase;

    /**
     * Setup a database for our application.
     *
     * @throws Exception Initialization exception.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupJNDI();
        migrateDatabase();
    }

    /**
     * Shut down the database.
     *
     * @throws Exception Teardown Exceptions.
     */
    @AfterClass
    public static void removeTestData() throws Exception {
        cleanDatabase();
    }

    /**
     * Setup the JNDI resource for our database tests.
     */
    private static void setupJNDI() {
        // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.eclipse.jetty.jndi.InitialContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES,
                "org.eclipse.jetty.jndi");

        try {
            InitialContext ic = new InitialContext();

            ic.createSubcontext("java:/comp/env");
            ic.createSubcontext("java:/comp/env/jdbc");

            JdbcConnectionPool ds = JdbcConnectionPool.create(
                    JDBC, "sa", "sa");
            ic.bind("java:/comp/env/jdbc/OIDServerDB", ds);
        } catch (NamingException ne) {
            ne.getMessage();
        }
    }

    /**
     * Migrate the database to update the schema.
     *
     * @throws Exception Liquibase migration exception.
     */
    private static void migrateDatabase() throws Exception {
        Class.forName("org.h2.Driver");
        conn = DriverManager
                .getConnection(JDBC, "sa", "sa");

        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(conn));

        liquibase = new Liquibase("liquibase/db.changelog-master.xml",
                new FileSystemResourceAccessor("src/main/resources"), database);
        liquibase.update(new Contexts());
    }

    /**
     * Clean the database.
     *
     * @throws Exception Liquibase migration exception.
     */
    private static void cleanDatabase() throws Exception {
        liquibase.rollback(1000, null);
        liquibase = null;

        conn.close();
        conn = null;
    }
}
