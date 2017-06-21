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
import net.krotscheck.kangaroo.common.cors.ExposedHeaders;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.internal.ServiceLocatorImpl;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Test that we can provide multiple sources of CORS allowed methods.
 *
 * @author Michael Krotscheck
 */
public class ExposedHeadersTest {
    /**
     * Assert that we can inject values using this binder
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testBinder() throws Exception {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocatorImpl locator = (ServiceLocatorImpl)
                factory.create(this.getClass().getSimpleName());

        Binder b1 = new ExposedHeaders(new String[]{"Test1", "Test2"});
        ServiceLocatorUtilities.bind(locator, b1);
        Binder b2 = new ExposedHeaders(new String[]{"Test3", "Test4"});
        ServiceLocatorUtilities.bind(locator, b2);

        Injectee testInjectee = new Injectee();

        locator.inject(testInjectee);

        List<String> values = Lists.newArrayList(testInjectee.getValues());
        Assert.assertTrue(values.contains("Test1"));
        Assert.assertTrue(values.contains("Test2"));
        Assert.assertTrue(values.contains("Test3"));
        Assert.assertTrue(values.contains("Test4"));
        Assert.assertEquals(4, values.size());
    }

    /**
     * Test class, used to inject our test data and then validate it.
     */
    public static final class Injectee {

        /**
         * The injected iterator of values.
         */
        private IterableProvider<String> values;

        /**
         * Get the values.
         *
         * @return The iterator, or null if not available.
         */
        public IterableProvider<String> getValues() {
            return values;
        }

        /**
         * Set the values.
         *
         * @param values An iterator of values.
         */
        @Inject
        public void setValues(@Named(ExposedHeaders.NAME) final
                              IterableProvider<String> values) {
            this.values = values;
        }
    }
}