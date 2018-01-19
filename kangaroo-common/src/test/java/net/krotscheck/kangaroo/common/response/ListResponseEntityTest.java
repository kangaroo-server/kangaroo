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

package net.krotscheck.kangaroo.common.response;

import net.krotscheck.kangaroo.common.hibernate.entity.TestEntity;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the list response entity.
 *
 * @author Michael Krotscheck
 */
public final class ListResponseEntityTest {

    /**
     * Test setting the total.
     */
    @Test
    public void getSetTotal() {
        ListResponseEntity<TestEntity> e = new ListResponseEntity<>();

        assertNull(e.getTotal());
        e.setTotal(10);
        assertEquals(10, e.getTotal());
    }

    /**
     * Test setting offset.
     */
    @Test
    public void getSetOffset() {
        ListResponseEntity<TestEntity> e = new ListResponseEntity<>();

        assertNull(e.getOffset());
        e.setOffset(10);
        assertEquals(10, e.getOffset());
    }

    /**
     * Test setting the limit.
     */
    @Test
    public void getSetLimit() {
        ListResponseEntity<TestEntity> e = new ListResponseEntity<>();

        assertNull(e.getLimit());
        e.setLimit(10);
        assertEquals(10, e.getLimit());
    }

    /**
     * Test setting the sort.
     */
    @Test
    public void getSetSort() {
        ListResponseEntity<TestEntity> e = new ListResponseEntity<>();

        assertNull(e.getSort());
        e.setSort("foo");
        assertEquals("foo", e.getSort());
    }

    /**
     * Test setting the order.
     */
    @Test
    public void getSetOrder() {
        ListResponseEntity<TestEntity> e = new ListResponseEntity<>();

        assertNull(e.getOrder());
        e.setOrder(SortOrder.ASC);
        assertEquals(SortOrder.ASC, e.getOrder());
    }

    /**
     * Test setting the results.
     */
    @Test
    public void getSetResults() {
        ListResponseEntity<TestEntity> e = new ListResponseEntity<>();
        List<TestEntity> list = new ArrayList<>();

        assertNull(e.getResults());
        e.setResults(list);
        assertEquals(list, e.getResults());
    }

}
