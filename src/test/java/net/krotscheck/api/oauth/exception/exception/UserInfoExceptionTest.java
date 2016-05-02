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

package net.krotscheck.api.oauth.exception.exception;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static net.krotscheck.api.oauth.exception.exception.UserInfoException.InsufficientScopeException;
import static net.krotscheck.api.oauth.exception.exception.UserInfoException.InvalidRequestException;
import static net.krotscheck.api.oauth.exception.exception.UserInfoException.InvalidTokenException;

/**
 * Test the exceptions, and their expected default properties.
 *
 * @author Michael Krotscheck
 */
public final class UserInfoExceptionTest {

    /**
     * Assert that the exception types provided all contain the expected
     * properties.
     */
    @Test
    public void testExpectedExceptionError() {
        Assert.assertEquals("invalid_request",
                new InvalidRequestException().getError());
        Assert.assertEquals("invalid_token",
                new InvalidTokenException().getError());
        Assert.assertEquals("insufficient_scope",
                new InsufficientScopeException().getError());
    }

    /**
     * Assert that the class is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = UserInfoException.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }
}
