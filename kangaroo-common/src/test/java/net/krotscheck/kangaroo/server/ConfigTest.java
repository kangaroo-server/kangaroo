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

package net.krotscheck.kangaroo.server;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Assert common config options are defaulted.
 *
 * @author Michael Krotscheck
 */
public final class ConfigTest {

    /**
     * Assert that the header is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = Config.class.getDeclaredConstructor();
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
        assertEquals("kangaroo.host", Config.HOST.getKey());
        assertEquals("127.0.0.1", Config.HOST.getValue());

        assertEquals("kangaroo.port", Config.PORT.getKey());
        assertEquals((Integer) 8080, Config.PORT.getValue());

        assertEquals("kangaroo.working_dir",
                Config.WORKING_DIR.getKey());
        assertEquals("/var/lib/kangaroo",
                Config.WORKING_DIR.getValue());

        assertEquals("kangaroo.keystore_path",
                Config.KEYSTORE_PATH.getKey());
        assertNull(Config.KEYSTORE_PATH.getValue());

        assertEquals("kangaroo.keystore_password",
                Config.KEYSTORE_PASS.getKey());
        assertEquals("kangaroo", Config.KEYSTORE_PASS.getValue());

        assertEquals("kangaroo.keystore_type",
                Config.KEYSTORE_TYPE.getKey());
        assertEquals("PKCS12", Config.KEYSTORE_TYPE.getValue());

        assertEquals("kangaroo.cert_alias",
                Config.CERT_ALIAS.getKey());
        assertEquals("kangaroo", Config.CERT_ALIAS.getValue());

        assertEquals("kangaroo.cert_key_password",
                Config.CERT_KEY_PASS.getKey());
        assertEquals("kangaroo", Config.CERT_KEY_PASS.getValue());

        assertEquals("kangaroo.html_app_root",
                Config.HTML_APP_ROOT.getKey());
        assertNull(Config.HTML_APP_ROOT.getValue());
    }
}
