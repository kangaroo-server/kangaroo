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

/**
 * Test the configuration entry entity.
 *
 * @author Michael Krotscheck
 */
public final class ConfigurationEntryTest {

    /**
     * Test getting/setting the section.
     */
    @Test
    public void testGetSetSection() {
        ConfigurationEntry entry = new ConfigurationEntry();

        Assert.assertNull(entry.getSection());
        entry.setSection("section");
        Assert.assertEquals("section", entry.getSection());
    }

    /**
     * Test getting/setting the key.
     */
    @Test
    public void testGetSetKey() {
        ConfigurationEntry entry = new ConfigurationEntry();

        Assert.assertNull(entry.getKey());
        entry.setKey("key");
        Assert.assertEquals("key", entry.getKey());
    }

    /**
     * Test getting/setting the value.
     */
    @Test
    public void testGetSetValue() {
        ConfigurationEntry entry = new ConfigurationEntry();

        Assert.assertNull(entry.getValue());
        entry.setValue("value");
        Assert.assertEquals("value", entry.getValue());
    }

    /**
     * Assert that the owner is null.
     */
    @Test
    public void testGetOwner() {
        ConfigurationEntry e = new ConfigurationEntry();
        Assert.assertNull(e.getOwner());
    }
}
