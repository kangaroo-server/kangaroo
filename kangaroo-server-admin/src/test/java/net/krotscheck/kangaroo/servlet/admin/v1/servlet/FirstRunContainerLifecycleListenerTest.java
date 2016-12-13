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

package net.krotscheck.kangaroo.servlet.admin.v1.servlet;

import net.krotscheck.kangaroo.database.config.HibernateConfiguration;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.FirstRunContainerLifecycleListener.Binder;
import net.krotscheck.kangaroo.test.DatabaseTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Singleton;

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
                        mockFactory, test);
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
                        getSessionFactory(), testConfig);
        l.onStartup(mockContainer);

        // Make sure we have an application ID.
        String appId = testConfig.getString(Config.APPLICATION_ID);
        UUID appUuid = UUID.fromString(appId);
        Assert.assertNotNull(appUuid);

        // Ensure that the app id can be resolved.
        Session s = getSession();
        Application application = s.get(Application.class, appUuid);
        Assert.assertNotNull(application);

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
                        getSessionFactory(), test);

        // Try shutdown.
        l.onShutdown(mockContainer);
        Mockito.verifyNoMoreInteractions(mockFactory);
        Mockito.verifyNoMoreInteractions(mockContainer);

        // Try reload.
        l.onReload(mockContainer);
        Mockito.verifyNoMoreInteractions(mockFactory);
        Mockito.verifyNoMoreInteractions(mockContainer);
    }

    /**
     * Assert that we can invoke the binder.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testBinder() throws Exception {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocator locator = factory.create(getClass().getCanonicalName());

        Binder b = new FirstRunContainerLifecycleListener.Binder();
        ServiceLocatorUtilities.bind(locator, b);

        List<ActiveDescriptor<?>> descriptors =
                locator.getDescriptors(
                        BuilderHelper.createContractFilter(
                                ContainerLifecycleListener.class.getName()));
        Assert.assertEquals(1, descriptors.size());

        ActiveDescriptor descriptor = descriptors.get(0);
        Assert.assertNotNull(descriptor);
        // Check scope...
        Assert.assertEquals(Singleton.class.getCanonicalName(),
                descriptor.getScope());

        // ... check name.
        Assert.assertNull(descriptor.getName());
    }
}
