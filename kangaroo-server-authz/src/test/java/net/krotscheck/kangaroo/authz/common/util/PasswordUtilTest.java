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

package net.krotscheck.kangaroo.authz.common.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the password utilities.
 *
 * @author Michael Krotscheck
 */
public final class PasswordUtilTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = PasswordUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that we can create a salt.
     */
    @Test
    public void testCreateSalt() {
        String salt = PasswordUtil.createSalt();
        assertEquals(44, salt.length());

        String salt2 = PasswordUtil.createSalt();
        assertNotEquals(salt, salt2);
    }

    /**
     * Test that we can hash a password, and validate that hash against itself.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testHashAndValidate() throws Exception {
        String password = RandomStringUtils.random(40);
        String salt1 = PasswordUtil.createSalt();
        String salt2 = PasswordUtil.createSalt();
        String hash1 = PasswordUtil.hash(password, salt1);

        assertTrue(PasswordUtil.isValid(password, salt1, hash1));
        assertFalse(PasswordUtil.isValid(password, salt2, hash1));
    }
}
