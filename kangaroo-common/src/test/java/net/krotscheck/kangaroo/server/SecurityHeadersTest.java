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

import com.google.common.net.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Unit test for our common security headers.
 *
 * @author Michael Krotscheck
 */
public class SecurityHeadersTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = SecurityHeaders.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that our expected headers are present.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testExpectedHeaders() throws Exception {
        Assert.assertEquals(1, SecurityHeaders.ALL.size());

        Assert.assertEquals("Deny",
                SecurityHeaders.ALL.get(HttpHeaders.X_FRAME_OPTIONS));
    }
}
