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

package net.krotscheck.kangaroo.common.httpClient;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for the client builder factory.
 *
 * @author Michael Krotscheck
 */
public final class JerseyClientBuilderFactoryTest {

    /**
     * Assert that the client builder can be created.
     */
    @Test
    public void assertCanCreateFactory() {
        JerseyClientBuilderFactory factory = new JerseyClientBuilderFactory();
        assertNotNull(factory.get());
    }

    /**
     * Assert that injection works.
     */
    @Test
    public void testInjection() {
        InjectionManager injector = Injections.createInjectionManager();
        injector.register(new JerseyClientBuilderFactory.Binder());
        injector.completeRegistration();

        JerseyClientBuilderFactory factory1 =
                injector.getInstance(JerseyClientBuilderFactory.class);
        JerseyClientBuilderFactory factory2 =
                injector.getInstance(JerseyClientBuilderFactory.class);

        assertSame(factory1, factory2);
        injector.shutdown();
    }
}
