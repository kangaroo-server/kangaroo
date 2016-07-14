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

package net.krotscheck.kangaroo.servlet.admin.v1;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Unit tests for our Scope constants.
 *
 * @author Michael Krotscheck
 */
public final class ScopeTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = Scope.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert the expected constant values.
     */
    @Test
    public void testExpectedConstants() {
        Assert.assertEquals("application", Scope.APPLICATION);
        Assert.assertEquals("authenticator", Scope.AUTHENTICATOR);
        Assert.assertEquals("client", Scope.CLIENT);
        Assert.assertEquals("user", Scope.USER);
        Assert.assertEquals("role", Scope.ROLE);
        Assert.assertEquals("identity", Scope.IDENTITY);
    }

    /**
     * Test that the list of scopes is all there.
     */
    @Test
    public void testAllScopeList() {
        List<String> allScopes = Scope.allScopes();

        Assert.assertTrue(allScopes.contains(Scope.APPLICATION));
        Assert.assertTrue(allScopes.contains(Scope.AUTHENTICATOR));
        Assert.assertTrue(allScopes.contains(Scope.CLIENT));
        Assert.assertTrue(allScopes.contains(Scope.USER));
        Assert.assertTrue(allScopes.contains(Scope.ROLE));
        Assert.assertTrue(allScopes.contains(Scope.IDENTITY));

        Assert.assertEquals(6, allScopes.size());
    }
}
