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

import com.mysql.jdbc.MySQLConnection;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This JUnit4 rule ensures that the JNDI resource has been bootstrapped, and
 * that the database schema has been migrated into the test database.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseResource extends AbstractDBRule {

    static {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.eclipse.jetty.jndi.InitialContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES,
                "org.eclipse.jetty.jndi");
    }

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(DatabaseResource.class);

    /**
     * Path to the liquibase changelog master file.
     */
    private static final String CHANGELOG = "liquibase/db" +
            ".changelog-master.yaml";

    /**
     * Evaluate the JNDI Identity.
     *
     * @return The JNDI Identity path.
     */
    private String getJndiAddress() {
        String jndiName = System.getProperty("test.db.jndiName",
                "OIDServerDB");
        return String.format("%s/%s", "java://comp/env/jdbc", jndiName);
    }

    /**
     * Set up a JDNI connection for your tests.
     *
     * @throws Throwable In case something odd happens.
     */
    private void setupJNDI() throws Throwable {
        InitialContext context = new InitialContext();
        ensureSubcontextExists(context);
        context.bind(getJndiAddress(), getDataSource());
    }

    /**
     * Clean up the JNDI Binding.
     *
     * @throws Throwable Thrown if something goes sideways in the context.
     */
    private void cleanJNDI() throws Throwable {
        // Clean anything opened in the initial context.
        InitialContext ctx = new InitialContext();
        ctx.unbind(getJndiAddress());
    }

    /**
     * Ensure the required subcontext namespaces exist on the provided context.
     *
     * @param context The context on which to create the subcontext.
     */
    private void ensureSubcontextExists(final Context context) {
        String[] names = new String[]{
                "java://comp",
                "java://comp/env",
                "java://comp/env/jdbc",
        };

        for (String name : names) {
            try {
                context.createSubcontext(name);
            } catch (NameAlreadyBoundException nae) {
                logger.trace(String.format("Subcontext [%s] already exists",
                        name));
            } catch (NamingException ne) {
                throw new RuntimeException(ne);
            }
        }
    }

    /**
     * Migrate the current database using the existing liquibase schema.
     *
     * @throws SQLException Exceptions thrown during migration. If these fail,
     *                      fix your tests!
     */
    private void setupDatabase() throws SQLException {
        logger.debug("Migrating Database Schema.");

        try (Connection conn = getDataSource().getConnection()) {

            // Flush the database if necessary
            if (conn instanceof MySQLConnection) {
                java.sql.Statement s = conn.createStatement();
                s.addBatch("DROP DATABASE IF EXISTS oid");
                s.addBatch("CREATE DATABASE IF NOT EXISTS oid;");
                s.executeBatch();
                s.close();
            }

            logger.debug("Migrating schema.");
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            Liquibase liquibase = new Liquibase(CHANGELOG,
                    new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());
        } catch (LiquibaseException lbe) {
            throw new RuntimeException(lbe);
        }
    }

    /**
     * Clean any previously established database schema.
     *
     * @throws Throwable An exception encountered during teardown.
     */
    private void cleanDatabase() throws Throwable {
        try (Connection conn = getDataSource().getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(
                            new JdbcConnection(conn));
            Liquibase l = new Liquibase(CHANGELOG,
                    new ClassLoaderResourceAccessor(),
                    database);
            l.rollback(1000, null);
        }
    }

    /**
     * Bind the datasource to JNDI, and migrate the database schema.
     *
     * @throws Throwable Exceptions thrown during setup.
     */
    @Override
    protected void before() throws Throwable {
        setupJNDI();
        setupDatabase();
    }

    /**
     * Rollback the database schema.
     *
     * @throws Throwable Exceptions thrown during setup.
     */
    @Override
    protected void after() throws Throwable {
        cleanDatabase();
        cleanJNDI();
    }
}
