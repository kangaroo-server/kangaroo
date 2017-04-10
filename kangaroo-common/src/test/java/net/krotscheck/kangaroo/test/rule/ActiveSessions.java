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

import com.mchange.v2.c3p0.PooledDataSource;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.sql.SQLException;

/**
 * This JUnit4 rule provides a few handy methods that allow one to mark(),
 * and then verify(), the current number of open connections against the
 * database.
 *
 * @author Michael Krotscheck
 */
public final class ActiveSessions implements TestRule {

    /**
     * The hibernate resource, which we use to grab our connection provider.
     */
    private final HibernateResource hibernateResource;

    /**
     * Create a new instance of the active sessions rule, connected to the
     * database created by the provided resource.
     *
     * @param hibernateResource The hibernate resource we use to grab the
     *                          pooled connection provider.
     */
    public ActiveSessions(final HibernateResource hibernateResource) {
        this.hibernateResource = hibernateResource;
    }

    /**
     * Retrieve the datasource from which we can calculate the active sessions.
     *
     * @return Data Source.
     */
    private PooledDataSource getDataSource() {
        SessionFactoryImpl sfi = (SessionFactoryImpl) hibernateResource
                .getSessionFactory();
        SessionFactoryServiceRegistryImpl src =
                (SessionFactoryServiceRegistryImpl) sfi.getServiceRegistry();
        C3P0ConnectionProvider cp = (C3P0ConnectionProvider)
                src.getService(ConnectionProvider.class);
        return cp.unwrap(PooledDataSource.class);
    }

    /**
     * Check that the previously marked number of active sessions has not
     * been modified.
     */
    private void check() {
        PooledDataSource ds = getDataSource();

        // Make sure we're starting clean.
        int unclosedOrphaned = 0;

        try {
            unclosedOrphaned =
                    ds.getNumUnclosedOrphanedConnectionsDefaultUser();
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals(String.format("%s Orphaned Connections found",
                unclosedOrphaned), 0, unclosedOrphaned);
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
                check();
                base.evaluate();
                check();
            }
        };
    }
}
