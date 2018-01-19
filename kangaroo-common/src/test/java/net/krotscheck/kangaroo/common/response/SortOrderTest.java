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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test the parameters for the sort order.
 *
 * @author Michael Krotscheck
 */
public final class SortOrderTest {

    /**
     * Identity test.
     */
    @Test
    public void testAllVariablesPresent() {
        assertNotNull(SortOrder.ASC);
        assertNotNull(SortOrder.DESC);
        assertEquals(2, SortOrder.values().length);
    }

    /**
     * Assert that asc de/parsing works.
     */
    @Test
    public void testAscParsing() {
        SortOrder upperCaseOrder = SortOrder.fromString("ASC");
        SortOrder lowerCaseOrder = SortOrder.fromString("asc");

        assertEquals(SortOrder.ASC, upperCaseOrder);
        assertEquals(SortOrder.ASC, lowerCaseOrder);
        assertEquals("ASC", SortOrder.ASC.toString());
    }

    /**
     * Assert that desc de/parsing works.
     */
    @Test
    public void testToString() {
        SortOrder upperCaseOrder = SortOrder.fromString("DESC");
        SortOrder lowerCaseOrder = SortOrder.fromString("desc");

        assertEquals(SortOrder.DESC, upperCaseOrder);
        assertEquals(SortOrder.DESC, lowerCaseOrder);
        assertEquals("DESC", SortOrder.DESC.toString());

    }

    /**
     * Assert that valueOf conversions works.
     */
    @Test
    public void testValueOf() {
        assertEquals(
                SortOrder.DESC,
                SortOrder.valueOf("DESC")
        );
        assertEquals(
                SortOrder.ASC,
                SortOrder.valueOf("ASC")
        );
    }
}
