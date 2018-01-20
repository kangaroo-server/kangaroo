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

package net.krotscheck.kangaroo.authz;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for the Authz Server configuration options.
 *
 * @author Michael Krotscheck
 */
public final class AuthzServerConfigTest {

    /**
     * Assert that the header is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = AuthzServerConfig.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that our config values are what we expect.
     */
    @Test
    public void testConfigurationValues() {
        assertEquals("kangaroo.authz_session_name",
                AuthzServerConfig.SESSION_NAME.getKey());
        assertEquals("kangaroo",
                AuthzServerConfig.SESSION_NAME.getValue());

        assertEquals("kangaroo.authz_session_max_age",
                AuthzServerConfig.SESSION_MAX_AGE.getKey());
        assertEquals((Integer) 86400,
                AuthzServerConfig.SESSION_MAX_AGE.getValue());
    }
}
