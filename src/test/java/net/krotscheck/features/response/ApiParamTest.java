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

/**
 * Test our API parameters.
 *
 * @author Michael Krotscheck
 */
public final class ApiParamTest {

    /**
     * Assert that our header values are all represented.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testApiParamProperties() throws Exception {
        Assert.assertEquals("Limit", ApiParam.LIMIT.headerName());
        Assert.assertEquals("limit", ApiParam.LIMIT.queryParam());
        Assert.assertEquals("10", ApiParam.LIMIT.queryDefault());

        Assert.assertEquals("Offset", ApiParam.OFFSET.headerName());
        Assert.assertEquals("offset", ApiParam.OFFSET.queryParam());
        Assert.assertEquals("0", ApiParam.OFFSET.queryDefault());

        Assert.assertEquals("Total", ApiParam.TOTAL.headerName());
        Assert.assertNull(ApiParam.TOTAL.queryParam());
        Assert.assertNull(ApiParam.TOTAL.queryDefault());

        Assert.assertEquals("Query", ApiParam.QUERY.headerName());
        Assert.assertEquals("q", ApiParam.QUERY.queryParam());
        Assert.assertEquals("", ApiParam.QUERY.queryDefault());

        Assert.assertEquals("Sort", ApiParam.SORT.headerName());
        Assert.assertEquals("sort", ApiParam.SORT.queryParam());
        Assert.assertEquals("id", ApiParam.SORT.queryDefault());

        Assert.assertEquals("Order", ApiParam.ORDER.headerName());
        Assert.assertEquals("order", ApiParam.ORDER.queryParam());
        Assert.assertEquals("ASC", ApiParam.ORDER.queryDefault());
    }
}