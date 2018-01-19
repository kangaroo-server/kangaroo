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

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

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
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that our config values are what we expect.
     */
    @Test
    public void testConfigurationValues() {
        Assert.assertEquals("kangaroo.host", Config.HOST.getKey());
        Assert.assertEquals("127.0.0.1", Config.HOST.getValue());

        Assert.assertEquals("kangaroo.port", Config.PORT.getKey());
        Assert.assertEquals((Integer) 8080, Config.PORT.getValue());

        Assert.assertEquals("kangaroo.working_dir",
                Config.WORKING_DIR.getKey());
        Assert.assertEquals("/var/lib/kangaroo",
                Config.WORKING_DIR.getValue());

        Assert.assertEquals("kangaroo.keystore_path",
                Config.KEYSTORE_PATH.getKey());
        Assert.assertNull(Config.KEYSTORE_PATH.getValue());

        Assert.assertEquals("kangaroo.keystore_password",
                Config.KEYSTORE_PASS.getKey());
        Assert.assertEquals("kangaroo", Config.KEYSTORE_PASS.getValue());

        Assert.assertEquals("kangaroo.keystore_type",
                Config.KEYSTORE_TYPE.getKey());
        Assert.assertEquals("PKCS12", Config.KEYSTORE_TYPE.getValue());

        Assert.assertEquals("kangaroo.cert_alias",
                Config.CERT_ALIAS.getKey());
        Assert.assertEquals("kangaroo", Config.CERT_ALIAS.getValue());

        Assert.assertEquals("kangaroo.cert_key_password",
                Config.CERT_KEY_PASS.getKey());
        Assert.assertEquals("kangaroo", Config.CERT_KEY_PASS.getValue());

        Assert.assertEquals("kangaroo.html_app_root",
                Config.HTML_APP_ROOT.getKey());
        Assert.assertNull(Config.HTML_APP_ROOT.getValue());
    }
}
