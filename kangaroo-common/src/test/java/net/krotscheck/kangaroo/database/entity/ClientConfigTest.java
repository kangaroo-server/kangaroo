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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Unit tests for our configuration defaults.
 *
 * @author Michael Krotscheck
 */
public final class ClientConfigTest {

    /**
     * Assert that the header is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = ClientConfig.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Test the config values for the authorization code.
     */
    @Test
    public void testAuthorizationCodeDefaults() {
        Integer expectedDefault = 60 * 10; // 10 minutes.
        Assert.assertEquals(expectedDefault,
                ClientConfig.AUTHORIZATION_CODE_EXPIRES_DEFAULT);
        Assert.assertEquals("authorization_code_expires_in",
                ClientConfig.AUTHORIZATION_CODE_EXPIRES_NAME);
    }

    /**
     * Test the config values for the access token.
     */
    @Test
    public void testAccessTokenDefaults() {
        Integer expectedDefault = 60 * 10; // 10 minutes.
        Assert.assertEquals(expectedDefault,
                ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT);
        Assert.assertEquals("access_token_expires_in",
                ClientConfig.ACCESS_TOKEN_EXPIRES_NAME);
    }

    /**
     * Test the config values for the refresh token.
     */
    @Test
    public void testRefreshTokenDefaults() {
        Integer expectedDefault = 60 * 60 * 24 * 30; // One month.
        Assert.assertEquals(expectedDefault,
                ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT);
        Assert.assertEquals("refresh_token_expires_in",
                ClientConfig.REFRESH_TOKEN_EXPIRES_NAME);
    }
}
