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

package net.krotscheck.kangaroo.database.entity;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.UUID;

/**
 * Unit tests for our abstract entity.
 */
public final class AbstractEntityTest {

    /**
     * Test ID get/set.
     */
    @Test
    public void testGetSetId() {
        UUID id = UUID.randomUUID();
        AbstractEntity a = new TestEntity();
        Assert.assertNull(a.getId());
        a.setId(id);
        Assert.assertEquals(id, a.getId());
    }

    /**
     * Test created date get/set.
     */
    @Test
    public void testGetSetCreatedDate() {
        AbstractEntity a = new TestEntity();
        Calendar d = Calendar.getInstance();

        Assert.assertNull(a.getCreatedDate());
        a.setCreatedDate(d);
        Assert.assertEquals(d, a.getCreatedDate());
        Assert.assertNotSame(d, a.getCreatedDate());
    }

    /**
     * Test created date get/set.
     */
    @Test
    public void testGetSetModifiedDate() {
        AbstractEntity a = new TestEntity();
        Calendar d = Calendar.getInstance();

        Assert.assertNull(a.getModifiedDate());
        a.setModifiedDate(d);
        Assert.assertEquals(d, a.getModifiedDate());
        Assert.assertNotSame(d, a.getModifiedDate());
    }

    /**
     * Test Equality by ID.
     */
    @Test
    public void testEquality() {
        UUID id = UUID.randomUUID();
        AbstractEntity a = new TestEntity();
        a.setId(id);

        AbstractEntity b = new TestEntity();
        b.setId(id);

        AbstractEntity c = new TestEntity();
        c.setId(UUID.randomUUID());

        AbstractEntity d = new TestEntity();

        AbstractEntity e = new TestEntity2();
        e.setId(UUID.randomUUID());

        Assert.assertTrue(a.equals(a));
        Assert.assertFalse(a.equals(null));
        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
        Assert.assertFalse(a.equals(c));
        Assert.assertFalse(c.equals(a));
        Assert.assertFalse(a.equals(d));
        Assert.assertFalse(d.equals(a));
        Assert.assertFalse(a.equals(e));
        Assert.assertFalse(e.equals(a));
    }

    /**
     * Test Equality by hashCode.
     */
    @Test
    public void testHashCode() {
        UUID id = UUID.randomUUID();
        AbstractEntity a = new TestEntity();
        a.setId(id);

        AbstractEntity b = new TestEntity();
        b.setId(id);

        AbstractEntity c = new TestEntity();
        c.setId(UUID.randomUUID());

        AbstractEntity d = new TestEntity();

        AbstractEntity e = new TestEntity2();
        e.setId(UUID.randomUUID());

        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertNotEquals(a.hashCode(), c.hashCode());
        Assert.assertNotEquals(a.hashCode(), d.hashCode());
        Assert.assertNotEquals(a.hashCode(), e.hashCode());
    }

    /**
     * Test toString.
     */
    @Test
    public void testToString() {
        AbstractEntity a = new TestEntity();
        a.setId(UUID.randomUUID());
        AbstractEntity b = new TestEntity();

        Assert.assertEquals(
                String.format("net.krotscheck.kangaroo.database.entity."
                                + "AbstractEntityTest.TestEntity [id=%s]",
                        a.getId()),
                a.toString());
        Assert.assertEquals("net.krotscheck.kangaroo.database.entity"
                + ".AbstractEntityTest.TestEntity [id=null]", b.toString());
    }

    /**
     * Test cloneable.
     *
     * @throws CloneNotSupportedException Should not be thrown.
     */
    @Test
    public void testCloneable() throws CloneNotSupportedException {
        AbstractEntity a = new TestEntity();
        a.setId(UUID.randomUUID());
        AbstractEntity b = (AbstractEntity) a.clone();

        Assert.assertEquals(a.getId(), b.getId());
    }

    /**
     * Test entity, used for testing!
     */
    private static class TestEntity extends AbstractEntity {

        /**
         * The owner of this entity.
         *
         * @return This entity's owner, if it exists.
         */
        @Override
        public User getOwner() {
            return null;
        }
    }

    /**
     * Another test entity, used for testing!
     */
    private static class TestEntity2 extends AbstractEntity {

        /**
         * The owner of this entity.
         *
         * @return This entity's owner, if it exists.
         */
        @Override
        public User getOwner() {
            return null;
        }
    }
}
