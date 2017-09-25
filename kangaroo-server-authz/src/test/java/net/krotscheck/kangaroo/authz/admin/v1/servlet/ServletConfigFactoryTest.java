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

import net.krotscheck.kangaroo.authz.common.authenticator.password.PasswordAuthenticator;
import net.krotscheck.kangaroo.common.config.SystemConfiguration;
import net.krotscheck.kangaroo.common.hibernate.config.HibernateConfiguration;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.apache.commons.configuration.Configuration;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Supplier;

import static junit.framework.TestCase.assertNotNull;

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
        Configuration created = factory.get();

        // Make sure it can access the data.
        Assert.assertEquals("property", created.getString("test"));
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
        Supplier factory = new ServletConfigFactory(getSessionFactory());
        Object instance = factory.get();
        Assert.assertTrue(instance instanceof HibernateConfiguration);
    }
}
