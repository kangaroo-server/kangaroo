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

import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory.Binder;
import net.krotscheck.kangaroo.common.hibernate.config.HibernateConfiguration;
import net.krotscheck.kangaroo.test.DatabaseTest;
import org.apache.commons.configuration.Configuration;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Singleton;
import java.util.List;

/**
 * Test that the admin configuration factory creates a singleton instance of
 * the HibernateConfiguration.
 */
public final class ServletConfigFactoryTest extends DatabaseTest {

    /**
     * Assert that we can create a configuration.
     */
    @Test
    public void testCreateConfiguration() {
        // Load some data.
        HibernateConfiguration compareConfig =
                new HibernateConfiguration(getSessionFactory(),
                        ServletConfigFactory.GROUP_NAME);
        compareConfig.addProperty("test", "property");

        // Create a new instance from the factory.
        ServletConfigFactory factory =
                new ServletConfigFactory(getSessionFactory());
        Configuration created = factory.provide();

        // Make sure it can access the data.
        Assert.assertEquals("property", created.getString("test"));
    }

    /**
     * Assert that disposing doesn't really do anything.
     */
    @Test
    public void testDispose() {
        // Create a new instance from the factory.
        ServletConfigFactory factory =
                new ServletConfigFactory(getSessionFactory());
        Configuration mock = Mockito.mock(Configuration.class);
        factory.dispose(mock);

        Mockito.verifyNoMoreInteractions(mock);
    }

    /**
     * Assert that the component is bound properly.
     */
    @Test
    public void testBinder() {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocator locator = factory.create(getClass().getCanonicalName());

        Binder b = new ServletConfigFactory.Binder();
        ServiceLocatorUtilities.bind(locator, b);

        List<ActiveDescriptor<?>> descriptors =
                locator.getDescriptors(
                        BuilderHelper.createNameAndContractFilter(
                                Configuration.class.getName(),
                                ServletConfigFactory.GROUP_NAME));
        Assert.assertEquals(1, descriptors.size());

        ActiveDescriptor descriptor = descriptors.get(0);
        Assert.assertNotNull(descriptor);
        // Check scope...
        Assert.assertEquals(Singleton.class.getCanonicalName(),
                descriptor.getScope());

        // ... check name.
        Assert.assertEquals(ServletConfigFactory.GROUP_NAME,
                descriptor.getName());
    }

    /**
     * Assert that the generic interface works as expected.
     *
     * @throws Exception Should not be thrown.
     * @see <a href="https://sourceforge.net/p/cobertura/bugs/92/">https://sourceforge.net/p/cobertura/bugs/92/</a>
     */
    @Test
    public void testGenericInterface() throws Exception {
        // Intentionally using the generic untyped interface here.
        Factory factory = new ServletConfigFactory(getSessionFactory());
        Object instance = factory.provide();
        Assert.assertTrue(instance instanceof HibernateConfiguration);
        factory.dispose(instance);
    }
}
