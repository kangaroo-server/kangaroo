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

package net.krotscheck.kangaroo.common.response;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertTrue(Modifier.isPrivate(c.getModifiers()));

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
        assertEquals("Limit", ApiParam.LIMIT_HEADER);
        assertEquals("limit", ApiParam.LIMIT_QUERY);
        assertEquals("10", ApiParam.LIMIT_DEFAULT);

        assertEquals("Offset", ApiParam.OFFSET_HEADER);
        assertEquals("offset", ApiParam.OFFSET_QUERY);
        assertEquals("0", ApiParam.OFFSET_DEFAULT);

        assertEquals("Total", ApiParam.TOTAL_HEADER);

        assertEquals("q", ApiParam.QUERY_QUERY);
        assertEquals("", ApiParam.QUERY_DEFAULT);

        assertEquals("Sort", ApiParam.SORT_HEADER);
        assertEquals("sort", ApiParam.SORT_QUERY);
        assertEquals("createdDate", ApiParam.SORT_DEFAULT);

        assertEquals("Order", ApiParam.ORDER_HEADER);
        assertEquals("order", ApiParam.ORDER_QUERY);
        assertEquals("ASC", ApiParam.ORDER_DEFAULT);
    }
}
