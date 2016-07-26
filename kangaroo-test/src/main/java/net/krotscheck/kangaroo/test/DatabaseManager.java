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

package net.krotscheck.kangaroo.test;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.TimeZone;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * A database manager which can be used in multiple tests.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseManager {

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
            LoggerFactory.getLogger(DatabaseManager.class);

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
        logger.info("Setting up JNDI.");
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
     * Internal session factory, reconstructed for every test run.
     */
    private SessionFactory sessionFactory;

    /**
     * The last created session.
     */
    private Session session;

    /**
     * Build, or retrieve, a session factory.
     *
     * @return The session factory.
     */
    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {

            ServiceRegistry serviceRegistry =
                    new StandardServiceRegistryBuilder()
                            .configure()
                            .build();

            sessionFactory = new MetadataSources(serviceRegistry)
                    .buildMetadata()
                    .buildSessionFactory();
        }
        return sessionFactory;
    }

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    public Session getSession() {
        if (session == null) {
            SessionFactory factory = getSessionFactory();
            session = factory.openSession();
        }
        return session;
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
     * Set up the liquibase search index.
     *
     * @throws InterruptedException Thrown if the search index construction
     *                              is interrupted.
     */
    public void buildSearchIndex() throws InterruptedException {
        // Create the fulltext session.
        FullTextSession fullTextSession =
                Search.getFullTextSession(getSession());

        fullTextSession
                .createIndexer()
                .startAndWait();
    }

    /**
     * Clean any previously established database schema.
     *
     * @throws Exception Exceptions thrown during migration. If these fail,
     *                   fix your tests!
     */
    public void cleanDatabase() throws Exception {
        if (liquibase != null) {
            logger.info("Cleaning Database.");
            liquibase.rollback(1000, null);
            liquibase = null;
        }

        if (conn != null) {
            logger.info("Closing connection.");
            conn.close();
            conn = null;
        }
    }

    /**
     * Clean all sessions.
     */
    public void cleanSessions() {
        if (session != null) {
            session.close();
            session = null;
        }

        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
