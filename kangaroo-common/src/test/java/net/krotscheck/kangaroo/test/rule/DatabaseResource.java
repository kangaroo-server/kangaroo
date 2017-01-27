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
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.krotscheck.kangaroo.test.TestConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * This JUnit4 rule ensures that the JNDI resource has been bootstrapped,
 * that a search index directory exists, and that the database schema has
 * been migrated into the test database.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseResource implements TestRule {

    static {
        // Force the database to use UTC.
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Make sure we use the tomcat context factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.naming.java.javaURLContextFactory");
    }

    /**
     * The initial context used in this rule.
     */
    private InitialContext context;

    /**
     * Get the JNDI Context.
     *
     * @return The initial context
     * @throws Throwable Some unexpected error.
     */
    private InitialContext getContext() throws Throwable {
        if (context == null) {
            context = new InitialContext();
        }
        return new InitialContext();
    }

    /**
     * Get the JNDI datasource.
     *
     * @return The datasource, from the JNDI catalog.
     * @throws Throwable Some unexpected error.
     */
    private BasicDataSource getDataSource() throws Throwable {
        return (BasicDataSource) getContext()
                .lookup(TestConfig.getDbJndiPath());
    }

    /**
     * Migrate the current database using the existing liquibase schema.
     *
     * @throws Throwable Exceptions thrown during migration. If these fail,
     *                   fix your tests!
     */
    private void setupDatabase() throws Throwable {
        try (Connection conn = getDataSource().getConnection()) {
            JdbcConnection connection = new JdbcConnection(conn);
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(connection);
            Liquibase liquibase = new Liquibase(TestConfig.getDbChangelog(),
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
            Liquibase l = new Liquibase(TestConfig.getDbChangelog(),
                    new ClassLoaderResourceAccessor(),
                    database);
            l.rollback(1000, null);
        }
    }

    /**
     * Set up a JDNI connection for your tests.
     *
     * @throws Throwable In case something odd happens.
     */
    private void setupJNDI() throws Throwable {
        String jndiPath = TestConfig.getDbJndiPath();
        InitialContext context = getContext();
        ensureSubcontextExists(context, jndiPath);

        // Bind the datasource.
        BasicDataSource dataSource = createDataSource();
        context.bind(jndiPath, dataSource);
    }

    /**
     * Clean up the JNDI Binding.
     *
     * @throws Throwable Thrown if something goes sideways in the context.
     */
    private void cleanJNDI() throws Throwable {
        InitialContext ctx = getContext();
        String jndiPath = TestConfig.getDbJndiPath();

        // Detach the datasource.
        BasicDataSource bds = (BasicDataSource) ctx.lookup(jndiPath);
        bds.close();

        // Clean anything opened in the initial context.
        ctx.unbind(jndiPath);
    }

    /**
     * Split the JNDI path into its hierarchical components. This does
     * include the final name, so you may have to carve that off manually.
     *
     * @param jndiPath The path.
     * @return The list of components, in order from general to specific.
     */
    private List<String> splitJndiPath(final String jndiPath) {
        List<String> results = new ArrayList<>();

        List<String> segments = Arrays.stream(jndiPath.split("/"))
                .filter(s -> s.length() > 0)
                .collect(Collectors.toList());

        while (segments.size() > 0) {
            // Get the first one.
            String nextSegment = segments.remove(0);

            if (results.size() == 0) {
                results.add(nextSegment);
            } else if (results.size() == 1) {
                results.add(String.format("%s//%s", results.get(0),
                        nextSegment));
            } else {
                results.add(String.format("%s/%s",
                        results.get(results.size() - 1),
                        nextSegment));
            }
        }
        return results;
    }

    /**
     * Ensure the required subcontext namespaces exist on the provided context.
     *
     * @param context  The context on which to create the subcontext.
     * @param jndiPath The JNDI Path.
     */
    private void ensureSubcontextExists(final Context context,
                                        final String jndiPath) {
        List<String> jndiSegments = splitJndiPath(jndiPath);

        // Remove the last one so we don't create a resource.
        jndiSegments.remove(jndiSegments.size() - 1);

        for (String name : jndiSegments) {
            try {
                context.createSubcontext(name);
            } catch (NameAlreadyBoundException nae) {
                // Do nothing.
            } catch (NamingException ne) {
                throw new RuntimeException(ne);
            }
        }
    }

    /**
     * Create a new datasource.
     *
     * @return The datasource.
     */
    private BasicDataSource createDataSource() {
        BasicDataSource newSource = new BasicDataSource();
        newSource.setDriverClassName(TestConfig.getDbDriver());
        newSource.setUrl(TestConfig.getDbJdbcPath());
        newSource.setUsername(TestConfig.getDbLogin());
        newSource.setPassword(TestConfig.getDbPassword());
        newSource.setMaxIdle(1);

        return newSource;
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
                    cleanJNDI();
                }
            }
        };
    }
}
