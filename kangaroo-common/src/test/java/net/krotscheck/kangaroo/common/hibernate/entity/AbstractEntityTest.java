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

package net.krotscheck.kangaroo.common.hibernate.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for our abstract entity.
 */
public final class AbstractEntityTest {

    /**
     * Object mapper, used for testing.
     */
    private static final ObjectMapper MAPPER =
            new ObjectMapperFactory().get();

    /**
     * Test re/serialization.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testSerialize() throws Exception {

        // Test entity.
        TestEntity a = new TestEntity();
        TestChildEntity c = new TestChildEntity();
        a.setId(IdUtil.next());
        c.setId(IdUtil.next());
        a.setChildEntity(c);

        String jsonString = MAPPER.writeValueAsString(a);
        TestEntity b = MAPPER.readValue(jsonString, TestEntity.class);

        assertEquals(a, b);
        assertEquals(
                a.getId(),
                b.getId());
        assertEquals(
                a.getChildEntity(),
                b.getChildEntity());
        assertEquals(
                a.getChildEntity().getId(),
                b.getChildEntity().getId());
    }

    /**
     * Test ID get/set.
     */
    @Test
    public void testGetSetId() {
        BigInteger id = IdUtil.next();
        AbstractEntity a = new TestEntity();
        assertNull(a.getId());
        a.setId(id);
        assertEquals(id, a.getId());
    }

    /**
     * Test created date get/set.
     */
    @Test
    public void testGetSetCreatedDate() {
        AbstractEntity a = new TestEntity();
        Calendar d = Calendar.getInstance();

        assertNull(a.getCreatedDate());
        a.setCreatedDate(d);
        assertEquals(d, a.getCreatedDate());
        assertNotSame(d, a.getCreatedDate());
    }

    /**
     * Test created date get/set.
     */
    @Test
    public void testGetSetModifiedDate() {
        AbstractEntity a = new TestEntity();
        Calendar d = Calendar.getInstance();

        assertNull(a.getModifiedDate());
        a.setModifiedDate(d);
        assertEquals(d, a.getModifiedDate());
        assertNotSame(d, a.getModifiedDate());
    }

    /**
     * Test Equality by ID.
     */
    @Test
    public void testEquality() {
        BigInteger id = IdUtil.next();
        AbstractEntity a = new TestEntity();
        a.setId(id);

        AbstractEntity b = new TestEntity();
        b.setId(id);

        AbstractEntity c = new TestEntity();
        c.setId(IdUtil.next());

        AbstractEntity d = new TestEntity();

        AbstractEntity e = new TestChildEntity();
        e.setId(IdUtil.next());

        assertTrue(a.equals(a));
        assertFalse(a.equals(null));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertFalse(a.equals(c));
        assertFalse(c.equals(a));
        assertFalse(a.equals(d));
        assertFalse(d.equals(a));
        assertFalse(a.equals(e));
        assertFalse(e.equals(a));
    }

    /**
     * Test Equality by hashCode.
     */
    @Test
    public void testHashCode() {
        BigInteger id = IdUtil.next();
        AbstractEntity a = new TestEntity();
        a.setId(id);

        AbstractEntity b = new TestEntity();
        b.setId(id);

        AbstractEntity c = new TestEntity();
        c.setId(IdUtil.next());

        AbstractEntity d = new TestEntity();

        AbstractEntity e = new TestChildEntity();
        e.setId(IdUtil.next());

        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
        assertNotEquals(a.hashCode(), d.hashCode());
        assertNotEquals(a.hashCode(), e.hashCode());
    }

    /**
     * Test toString.
     */
    @Test
    public void testToString() {
        AbstractEntity a = new TestEntity();
        a.setId(IdUtil.next());
        AbstractEntity b = new TestEntity();

        assertEquals(
                String.format("net.krotscheck.kangaroo.common.hibernate.entity"
                                + ".TestEntity [id=%s]",
                        IdUtil.toString(a.getId())),
                a.toString());
        assertEquals("net.krotscheck.kangaroo.common"
                + ".hibernate.entity.TestEntity"
                + " [id=null]", b.toString());
    }

    /**
     * Test cloneable.
     *
     * @throws CloneNotSupportedException Should not be thrown.
     */
    @Test
    public void testCloneable() throws CloneNotSupportedException {
        BigInteger id = IdUtil.next();
        AbstractEntity a = new TestEntity();
        a.setId(id);
        AbstractEntity b = (AbstractEntity) a.clone();

        assertEquals(a.getId(), b.getId());
    }
}
