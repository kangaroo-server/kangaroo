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

package net.krotscheck.kangaroo.common.hibernate.id;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * ID utility tests.
 *
 * @author Michael Krotscheck
 */
public final class IdUtilTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = IdUtil.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that we can convert an ID to a string and back.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testIdStringConvert() throws Exception {
        // Test this 1000 times.
        for (int i = 0; i < 1000; i++) {
            BigInteger id = IdUtil.next();
            String idString = IdUtil.toString(id);

            // They must match.
            assertEquals(id, IdUtil.fromString(idString));

            // The string must be 32 characters long.
            assertEquals(String.valueOf(i), 32, idString.length());
        }
    }

    /**
     * Assert that null values are passed straight back out.
     */
    @Test
    public void testNullSafeStringConvert() {
        assertNull(IdUtil.toString(null));
        assertNull(IdUtil.fromString(null));
    }

    /**
     * Assert that a malformed string cannot be converted back to a BigInteger.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMalformedIdFromString() {
        IdUtil.fromString("notBase16String");
    }
}
