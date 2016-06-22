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

/**
 * Unit tests for the error codes defined in RFC6749.
 *
 * @author Michael Krotscheck
 */
public final class ErrorCodeTest {

    /**
     * Assert the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = ErrorCode.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that our header values are all represented.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testErrorCodeProperties() throws Exception {
        Assert.assertEquals("access_denied",
                ErrorCode.ACCESS_DENIED);
        Assert.assertEquals("invalid_client",
                ErrorCode.INVALID_CLIENT);
        Assert.assertEquals("invalid_grant",
                ErrorCode.INVALID_GRANT);

        Assert.assertEquals("invalid_request",
                ErrorCode.INVALID_REQUEST);
        Assert.assertEquals("invalid_scope",
                ErrorCode.INVALID_SCOPE);
        Assert.assertEquals("server_error",
                ErrorCode.SERVER_ERROR);

        Assert.assertEquals("temporarily_unavailable",
                ErrorCode.TEMPORARILY_UNAVAILABLE);

        Assert.assertEquals("unauthorized_client",
                ErrorCode.UNAUTHORIZED_CLIENT);
        Assert.assertEquals("unsupported_grant_type",
                ErrorCode.UNSUPPORTED_GRANT_TYPE);
        Assert.assertEquals("unsupported_response_type",
                ErrorCode.UNSUPPORTED_RESPONSE_TYPE);
    }
}
