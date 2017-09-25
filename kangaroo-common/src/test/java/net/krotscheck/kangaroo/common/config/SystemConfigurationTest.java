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

package net.krotscheck.kangaroo.common.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the system configuration component.
 *
 * @author Michael Krotscheck
 */
public final class SystemConfigurationTest {

    /**
     * Test that with no injectees, we simply pass in the system configuraiton.
     */
    @Test
    public void testConfigurationDefaults() {
        SystemConfiguration config =
                new SystemConfiguration(Collections.emptyList());

        assertEquals(TimeZone.getTimeZone("UTC"), config.getTimezone());

        Configuration c =
                new org.apache.commons.configuration.SystemConfiguration();
        c.getKeys().forEachRemaining(s -> assertTrue(s, config.containsKey(s)));
    }

    /**
     * Test that we can inject new configuration values.
     */
    @Test
    public void testConfigurationInjection() {
        Properties properties = new Properties();
        properties.setProperty("foo", "bar");
        MapConfiguration mapConfig = new MapConfiguration(properties);

        SystemConfiguration config =
                new SystemConfiguration(Collections.singletonList(mapConfig));

        properties.forEach((k, v) -> assertEquals(v,
                config.getString(k.toString())));
    }
}
