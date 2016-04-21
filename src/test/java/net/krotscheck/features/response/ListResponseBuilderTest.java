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

import net.krotscheck.features.response.ListResponseBuilder.HttpHeaders;
import net.krotscheck.features.response.ListResponseBuilder.SortOrder;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

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
        Assert.assertEquals(entity, response.getEntity());
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.OFFSET));
        Assert.assertEquals(HttpHeaders.OFFSET,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.OFFSET));
        Assert.assertEquals(HttpHeaders.OFFSET,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.OFFSET));
        Assert.assertEquals(HttpHeaders.OFFSET,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.LIMIT));
        Assert.assertEquals(HttpHeaders.LIMIT,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.LIMIT));
        Assert.assertEquals(HttpHeaders.LIMIT,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.LIMIT));
        Assert.assertEquals(HttpHeaders.LIMIT,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.TOTAL));
        Assert.assertEquals(HttpHeaders.TOTAL,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.TOTAL));
        Assert.assertEquals(HttpHeaders.TOTAL,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("10",
                response.getHeaderString(HttpHeaders.TOTAL));
        Assert.assertEquals(HttpHeaders.TOTAL,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("foo",
                response.getHeaderString(HttpHeaders.SORT_ON));
        Assert.assertEquals(HttpHeaders.SORT_ON,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("DESC",
                response.getHeaderString(HttpHeaders.SORT_ORDER));
        Assert.assertEquals(HttpHeaders.SORT_ORDER,
                response.getHeaderString("Vary"));
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

        Assert.assertEquals("DESC",
                response.getHeaderString(HttpHeaders.SORT_ORDER));
        Assert.assertEquals(HttpHeaders.SORT_ORDER,
                response.getHeaderString("Vary"));
    }

    /**
     * Assert that you can apply a search query.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testQuery() throws Exception {
        ListResponseBuilder b = ListResponseBuilder.builder();
        b.query("foo");
        Response response = b.build();

        Assert.assertEquals("foo",
                response.getHeaderString(HttpHeaders.QUERY));
        Assert.assertEquals(HttpHeaders.QUERY,
                response.getHeaderString("Vary"));
    }

    /**
     * Assert that SortOrder.fromString always works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testSortFromString() throws Exception {
        Assert.assertEquals(SortOrder.ASC,
                SortOrder.fromString("ASC"));
        Assert.assertEquals(SortOrder.DESC,
                SortOrder.fromString("DESC"));
        Assert.assertEquals(SortOrder.ASC,
                SortOrder.fromString("invalid"));
    }

    /**
     * Assert that SortOrder.fromString always works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testSortToString() throws Exception {
        Assert.assertEquals("ASC", SortOrder.ASC.toString());
        Assert.assertEquals("DESC", SortOrder.DESC.toString());
    }

    /**
     * Assert that the HTTPHeaders class has a private constructor.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testHttpHeaderPrivate() throws Exception {
        Constructor<HttpHeaders> constructor =
                HttpHeaders.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    /**
     * Assert that our header values are as expected.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testHttpHeaderProperties() throws Exception {
        Assert.assertEquals("Sort-On", HttpHeaders.SORT_ON);
        Assert.assertEquals("Sort-Order", HttpHeaders.SORT_ORDER);
        Assert.assertEquals("Offset", HttpHeaders.OFFSET);
        Assert.assertEquals("Limit", HttpHeaders.LIMIT);
        Assert.assertEquals("Total", HttpHeaders.TOTAL);
        Assert.assertEquals("Query", HttpHeaders.QUERY);
    }
}
