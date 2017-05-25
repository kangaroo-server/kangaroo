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

package net.krotscheck.kangaroo.authz.common.database.util;

import net.krotscheck.kangaroo.common.response.SortOrder;
import org.hibernate.criterion.Order;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * A hibernate sort utility.
 *
 * @author Michael Krotscheck
 */
public final class SortUtilTest {

    /**
     * Test that the utility translates our constants into hibernate Order
     * instances.
     */
    @Test
    public void testOrder() {
        Order order = SortUtil.order(SortOrder.ASC, "foo");
        Assert.assertEquals("foo", order.getPropertyName());
        Assert.assertEquals(true, order.isAscending());

        Order order2 = SortUtil.order(SortOrder.DESC, "bar");
        Assert.assertEquals("bar", order2.getPropertyName());
        Assert.assertEquals(false, order2.isAscending());
    }

    /**
     * Assert that the header is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = SortUtil.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }
}
