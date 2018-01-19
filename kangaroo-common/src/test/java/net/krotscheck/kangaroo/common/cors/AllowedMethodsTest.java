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

package net.krotscheck.kangaroo.common.cors;

import com.google.common.collect.Lists;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Test that we can provide multiple sources of CORS allowed methods.
 *
 * @author Michael Krotscheck
 */
public final class AllowedMethodsTest {

    /**
     * Assert that we can inject values using this binder.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testBinder() throws Exception {
        InjectionManager injector = Injections.createInjectionManager();

        Binder b1 = new AllowedMethods(new String[]{"Test1", "Test2"});
        Binder b2 = new AllowedMethods(new String[]{"Test3", "Test4"});
        injector.register(b1);
        injector.register(b2);

        Injectee testInjectee = new Injectee();

        injector.inject(testInjectee);

        List<String> values = Lists.newArrayList(testInjectee.getValues());
        Assert.assertTrue(values.contains("Test1"));
        Assert.assertTrue(values.contains("Test2"));
        Assert.assertTrue(values.contains("Test3"));
        Assert.assertTrue(values.contains("Test4"));
        Assert.assertEquals(4, values.size());

        injector.shutdown();
    }

    /**
     * Test class, used to inject our test data and then validate it.
     */
    public static final class Injectee {

        /**
         * The injected iterator of values.
         */
        private Iterable<String> values;

        /**
         * Get the values.
         *
         * @return The iterator, or null if not available.
         */
        public Iterable<String> getValues() {
            return values;
        }

        /**
         * Set the values.
         *
         * @param values An iterator of values.
         */
        @Inject
        public void setValues(@Named(AllowedMethods.NAME) final
                              Iterable<String> values) {
            this.values = values;
        }
    }
}
