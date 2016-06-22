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

package net.krotscheck.features.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.TimeZone;
import javax.servlet.ServletContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the system configuration component.
 *
 * @author Michael Krotscheck
 */
public final class SystemConfigurationTest {

    /**
     * Test the configuration that's detected.
     */
    @Test
    public void testConfigurationDefaults() {
        ServletContext context = mock(ServletContext.class);

        SystemConfiguration config = new SystemConfiguration(context);

        Assert.assertEquals("dev", config.getVersion());
        Assert.assertEquals(TimeZone.getTimeZone("UTC"), config.getTimezone());
    }

    /**
     * Test that a file configuration is parsed.
     */
    @Test
    public void testConfigurationFromFile() {
        ServletContext context = mock(ServletContext.class);
        when(context.getRealPath("/META-INF/MANIFEST.MF"))
                .thenReturn("src/test/resources/META-INF/MANIFEST.MF");

        SystemConfiguration config = new SystemConfiguration(context);

        Assert.assertEquals("test", config.getVersion());
    }
}
