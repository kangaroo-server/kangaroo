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

import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidClientException;
import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidGrantException;
import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.UnsupportedGrantTypeException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.AccessDeniedException;
import static net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidRequestException;
import static net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidScopeException;
import static net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.ServerErrorException;
import static net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.TemporarilyUnavailableException;
import static net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.UnauthorizedClientException;
import static net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.UnsupportedResponseType;

/**
 * Test the exceptions, and their expected default properties.
 *
 * @author Michael Krotscheck
 */
public final class Rfc6749ExceptionTest {

    /**
     * Assert that the exception types provided all contain the expected
     * properties.
     */
    @Test
    public void testExpectedExceptionError() {
        Assert.assertEquals(ErrorCode.INVALID_REQUEST,
                new InvalidRequestException().getError());
        Assert.assertEquals(ErrorCode.UNAUTHORIZED_CLIENT,
                new UnauthorizedClientException().getError());
        Assert.assertEquals(ErrorCode.ACCESS_DENIED,
                new AccessDeniedException().getError());
        Assert.assertEquals(ErrorCode.UNSUPPORTED_RESPONSE_TYPE,
                new UnsupportedResponseType().getError());
        Assert.assertEquals(ErrorCode.INVALID_SCOPE,
                new InvalidScopeException().getError());
        Assert.assertEquals(ErrorCode.SERVER_ERROR,
                new ServerErrorException().getError());
        Assert.assertEquals(ErrorCode.TEMPORARILY_UNAVAILABLE,
                new TemporarilyUnavailableException().getError());
        Assert.assertEquals(ErrorCode.UNSUPPORTED_GRANT_TYPE,
                new UnsupportedGrantTypeException().getError());
        Assert.assertEquals(ErrorCode.INVALID_GRANT,
                new InvalidGrantException().getError());
        Assert.assertEquals(ErrorCode.INVALID_CLIENT,
                new InvalidClientException().getError());
    }

    /**
     * Assert that the class is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = Rfc6749Exception.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }
}
