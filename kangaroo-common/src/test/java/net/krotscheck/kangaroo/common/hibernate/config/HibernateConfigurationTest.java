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

package net.krotscheck.kangaroo.common.hibernate.config;

import net.krotscheck.kangaroo.test.DatabaseTest;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the Hibernate-backed configuration implementation.
 *
 * @author Michael Krotscheck
 */
public final class HibernateConfigurationTest extends DatabaseTest {

    /**
     * The configuration instance under test.
     */
    private Configuration configOne;

    /**
     * A second instance under test.
     */
    private Configuration configTwo;

    /**
     * Setup the test.
     */
    @Before
    public void setup() {
        configOne = new HibernateConfiguration(
                getSessionFactory(),
                "one");
        configTwo = new HibernateConfiguration(
                getSessionFactory(),
                "two");
    }

    /**
     * Teardown the test.
     */
    @After
    public void teardown() {
        configOne.clear();
        configTwo.clear();
    }

    /**
     * Test isEmpty().
     */
    @Test
    public void testIsEmpty() {
        Assert.assertTrue(configOne.isEmpty());
        configOne.addProperty("key", "value");
        Assert.assertFalse(configOne.isEmpty());
    }

    /**
     * Test keys.
     */
    @Test
    public void testKeys() {
        configOne.addProperty("key", "value");
        configOne.addProperty("lol", "cat");

        List<String> keys = new ArrayList<>();
        configOne.getKeys().forEachRemaining(keys::add);

        Assert.assertTrue(keys.contains("key"));
        Assert.assertTrue(keys.contains("lol"));
        Assert.assertEquals(2, keys.size());
    }

    /**
     * Assert that we can store and retrieve data.
     */
    @Test
    public void testStoreAndRetrieve() {
        configOne.addProperty("key", "value");
        Assert.assertTrue(configOne.containsKey("key"));
        Assert.assertEquals("value", configOne.getString("key"));
        configOne.clearProperty("key");
        Assert.assertFalse(configOne.containsKey("key"));
    }

    /**
     * Assert that multiple instances read from the same source.
     */
    @Test
    public void testMultipleInstances() {
        configOne.addProperty("key", "value");
        Assert.assertTrue(configOne.containsKey("key"));

        Configuration configuration = new HibernateConfiguration(
                getSessionFactory(),
                "one");
        Assert.assertTrue(configuration.containsKey("key"));
    }

    /**
     * Assert that separate instances do not conflict.
     */
    @Test
    public void testNoGroupConflict() {
        configOne.addProperty("key", "value");
        Assert.assertTrue(configOne.containsKey("key"));
        Assert.assertFalse(configTwo.containsKey("key"));

        configTwo.addProperty("foo", "bar");
        Assert.assertFalse(configOne.containsKey("foo"));
        Assert.assertTrue(configTwo.containsKey("foo"));
    }
}
