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

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for our list response builder.
 */
public final class ListResponseBuilderTest {

    /**
     * Assert that we can add an entity.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testAddEntity() throws Exception {
        List<String> entity = new ArrayList<>();

        ListResponseBuilder b = ListResponseBuilder.builder();
        b.addResult(entity);
        Response response = b.build();
        ListResponseEntity e = (ListResponseEntity) response.getEntity();
        assertEquals(entity, e.getResults());
    }

    /**
     * Assert that adding an integer offset works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testIntegerOffset() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.offset(Integer.valueOf(10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.OFFSET_HEADER));
    }

    /**
     * Assert that adding a long offset works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testLongOffset() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.offset(Long.valueOf(10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.OFFSET_HEADER));
    }

    /**
     * Assert that adding a short offset works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testShortOffset() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.offset(Short.valueOf((short) 10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.OFFSET_HEADER));
    }

    /**
     * Assert that adding an integer limit works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testIntegerLimit() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.limit(Integer.valueOf(10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.LIMIT_HEADER));
    }

    /**
     * Assert that adding a long limit works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testLongLimit() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.limit(Long.valueOf(10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.LIMIT_HEADER));
    }

    /**
     * Assert that adding a short limit works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testShortLimit() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.limit(Short.valueOf((short) 10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.LIMIT_HEADER));
    }

    /**
     * Assert that adding an integer total works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testIntegerTotal() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.total(Integer.valueOf(10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.TOTAL_HEADER));
    }

    /**
     * Assert that adding an object total works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testObjectTotal() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.total((Object) Integer.valueOf(10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.TOTAL_HEADER));
    }

    /**
     * Assert that adding a long total works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testLongTotal() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.total(Long.valueOf(10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.TOTAL_HEADER));
    }

    /**
     * Assert that adding a short total works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testShortTotal() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.total(Short.valueOf((short) 10));
        Response response = b.build();

        assertEquals("10",
                response.getHeaderString(ApiParam.TOTAL_HEADER));
    }

    /**
     * Assert that you can apply a sort parameter.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testSort() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.sort("foo");
        Response response = b.build();

        assertEquals("foo",
                response.getHeaderString(ApiParam.SORT_HEADER));
    }

    /**
     * Assert that you can apply a sort order string.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testSortOrderString() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.order("DESC");
        Response response = b.build();

        assertEquals("DESC",
                response.getHeaderString(ApiParam.ORDER_HEADER));
    }

    /**
     * Assert that you can apply a sort order enum.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testSortOrderEnum() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.order(SortOrder.DESC);
        Response response = b.build();

        assertEquals("DESC",
                response.getHeaderString(ApiParam.ORDER_HEADER));
    }

    /**
     * Assert that SortOrder.fromString always works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testSortFromString() throws Exception {
        assertEquals(SortOrder.ASC,
                SortOrder.fromString("ASC"));
        assertEquals(SortOrder.DESC,
                SortOrder.fromString("DESC"));
        assertEquals(SortOrder.ASC,
                SortOrder.fromString("invalid"));
    }

    /**
     * Assert that SortOrder.fromString always works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testSortToString() throws Exception {
        assertEquals("ASC", SortOrder.ASC.toString());
        assertEquals("DESC", SortOrder.DESC.toString());
    }
}
