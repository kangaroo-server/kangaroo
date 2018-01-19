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

package net.krotscheck.kangaroo.authz.admin.v1.servlet;

import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.common.hibernate.config.HibernateConfiguration;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.hibernate.migration.DatabaseMigrationState;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test our application bootstrap.
 *
 * @author Michael Krotscheck
 */
public final class FirstRunContainerLifecycleListenerTest
        extends DatabaseTest {

    /**
     * Make sure that the lifecycle listener is only run when the firstRun
     * flag is called.
     */
    @Test
    public void assertOnlyRunOnce() {
        Properties p = new Properties();
        p.put(Config.FIRST_RUN, true);
        Configuration test = new MapConfiguration(p);
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        Container mockContainer = Mockito.mock(Container.class);

        ContainerLifecycleListener l =
                new FirstRunContainerLifecycleListener(
                        mockFactory, test, new DatabaseMigrationState());
        l.onStartup(mockContainer);

        Mockito.verifyNoMoreInteractions(mockFactory);
        Mockito.verifyNoMoreInteractions(mockContainer);
    }

    /**
     * An application should be bootstrapped.
     */
    @Test
    public void assertBootstrapSuccessful() {
        Configuration testConfig = new HibernateConfiguration(
                getSessionFactory(), ServletConfigFactory.GROUP_NAME);
        Container mockContainer = Mockito.mock(Container.class);

        ContainerLifecycleListener l =
                new FirstRunContainerLifecycleListener(
                        getSessionFactory(), testConfig,
                        new DatabaseMigrationState());
        l.onStartup(mockContainer);

        // Grab the session.
        Session s = getSession();

        // Make sure we have an application ID.
        String appId = testConfig.getString(Config.APPLICATION_ID);
        BigInteger appByte = IdUtil.fromString(appId);
        assertNotNull(appByte);

        // Ensure that the app id can be resolved.
        Application application = s.get(Application.class, appByte);
        assertNotNull(application);

        // Make sure we have an application client ID
        String clientId = testConfig.getString(Config.APPLICATION_CLIENT_ID);
        BigInteger clientByte = IdUtil.fromString(clientId);
        assertNotNull(clientByte);

        // Ensure that the client id can be resolved.
        Client client = s.get(Client.class, clientByte);
        assertNotNull(client);

        // Make sure we have an application user ID
        String adminId = testConfig.getString(Config.APPLICATION_ADMIN_ID);
        BigInteger adminByte = IdUtil.fromString(adminId);
        assertNotNull(adminByte);

        // Ensure that the client id can be resolved.
        User user = s.get(User.class, adminByte);
        assertNotNull(user);

        // Cleanup
        testConfig.clear();
    }

    /**
     * Make sure that nothing happens in the shutdown or reload actions.
     */
    @Test
    public void assertReloadShutdownNoInteraction() {
        Properties p = new Properties();
        p.put(Config.FIRST_RUN, true);
        Configuration test = new MapConfiguration(p);
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        Container mockContainer = Mockito.mock(Container.class);

        ContainerLifecycleListener l =
                new FirstRunContainerLifecycleListener(
                        getSessionFactory(), test,
                        new DatabaseMigrationState());

        // Try shutdown.
        l.onShutdown(mockContainer);
        Mockito.verifyNoMoreInteractions(mockFactory);
        Mockito.verifyNoMoreInteractions(mockContainer);

        // Try reload.
        l.onReload(mockContainer);
        Mockito.verifyNoMoreInteractions(mockFactory);
        Mockito.verifyNoMoreInteractions(mockContainer);
    }
}
