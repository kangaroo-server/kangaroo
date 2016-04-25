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

package net.krotscheck.features.response;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Test our API parameters.
 *
 * @author Michael Krotscheck
 */
public final class ApiParamTest {

    /**
     * Assert the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = ApiParam.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that our header values are all represented.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testApiParamProperties() throws Exception {
        Assert.assertEquals("Limit", ApiParam.LIMIT_HEADER);
        Assert.assertEquals("limit", ApiParam.LIMIT_QUERY);
        Assert.assertEquals("10", ApiParam.LIMIT_DEFAULT);

        Assert.assertEquals("Offset", ApiParam.OFFSET_HEADER);
        Assert.assertEquals("offset", ApiParam.OFFSET_QUERY);
        Assert.assertEquals("0", ApiParam.OFFSET_DEFAULT);

        Assert.assertEquals("Total", ApiParam.TOTAL_HEADER);

        Assert.assertEquals("Query", ApiParam.QUERY_HEADER);
        Assert.assertEquals("q", ApiParam.QUERY_QUERY);
        Assert.assertEquals("", ApiParam.QUERY_DEFAULT);

        Assert.assertEquals("Sort", ApiParam.SORT_HEADER);
        Assert.assertEquals("sort", ApiParam.SORT_QUERY);
        Assert.assertEquals("id", ApiParam.SORT_DEFAULT);

        Assert.assertEquals("Order", ApiParam.ORDER_HEADER);
        Assert.assertEquals("order", ApiParam.ORDER_QUERY);
        Assert.assertEquals("ASC", ApiParam.ORDER_DEFAULT);
    }
}
