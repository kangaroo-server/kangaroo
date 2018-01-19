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

package net.krotscheck.kangaroo.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static net.krotscheck.kangaroo.util.ObjectUtil.safeCast;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for our object utilities.
 *
 * @author Michael Krotscheck
 */
public final class ObjectUtilTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = ObjectUtil.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that we can safely cast an interface to an implementing class.
     */
    @Test
    public void testSafeCastSimple() {
        Number testValue = new Integer(10);
        Integer targetValue = safeCast(testValue, Integer.class)
                .orElse(null);

        assertSame(testValue, targetValue);
    }

    /**
     * Assert that a failed cast does not throw an exception.
     */
    @Test
    public void testFailedCastNoError() {
        Object testValue = new Object();
        Integer targetValue = safeCast(testValue, Integer.class)
                .orElse(null);

        assertNull(targetValue);
    }
}
