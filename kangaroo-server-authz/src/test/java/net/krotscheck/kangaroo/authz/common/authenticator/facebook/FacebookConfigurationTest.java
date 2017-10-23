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

package net.krotscheck.kangaroo.authz.common.authenticator.facebook;

import net.krotscheck.kangaroo.authz.common.authenticator.exception.MisconfiguredAuthenticatorException;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for our facebook configuration parser.
 *
 * @author Michael Krotscheck
 */
public final class FacebookConfigurationTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = FacebookConfiguration.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that passing in a null authenticator fails
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testFromNullInput() throws Exception {
        FacebookConfiguration.from(null);
    }

    /**
     * Assert that passing in a null configuration fails
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testFromNullConfig() throws Exception {
        Authenticator auth = new Authenticator();
        auth.setConfiguration(null);
        FacebookConfiguration.from(auth);
    }

    /**
     * Assert that passing in an empty configuration fails
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testFromEmptyConfig() throws Exception {
        Authenticator auth = new Authenticator();
        FacebookConfiguration.from(auth);
    }

    /**
     * Assert that passing in a config with no app id fails.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testFromNoAppId() throws Exception {
        Authenticator auth = new Authenticator();
        auth.getConfiguration()
                .put(FacebookConfiguration.CLIENT_SECRET_KEY, "foo");
        FacebookConfiguration.from(auth);
    }

    /**
     * Assert that passing in a config with no app secret fails.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testFromNoAppSecret() throws Exception {
        Authenticator auth = new Authenticator();
        auth.getConfiguration()
                .put(FacebookConfiguration.CLIENT_ID_KEY, "foo");
        FacebookConfiguration.from(auth);
    }

    /**
     * Assert that passing in a valid config passes.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testFrom() throws Exception {
        Authenticator auth = new Authenticator();
        auth.getConfiguration()
                .put(FacebookConfiguration.CLIENT_ID_KEY, "foo");
        auth.getConfiguration()
                .put(FacebookConfiguration.CLIENT_SECRET_KEY, "bar");
        FacebookConfiguration config = FacebookConfiguration.from(auth);

        assertEquals(config.getClientId(), "foo");
        assertEquals(config.getClientSecret(), "bar");
    }
}