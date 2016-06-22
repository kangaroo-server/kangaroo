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
import java.net.URI;
import javax.ws.rs.core.UriBuilder;

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
                new InvalidRequestException().getErrorCode());
        Assert.assertEquals(ErrorCode.UNAUTHORIZED_CLIENT,
                new UnauthorizedClientException().getErrorCode());
        Assert.assertEquals(ErrorCode.ACCESS_DENIED,
                new AccessDeniedException().getErrorCode());
        Assert.assertEquals(ErrorCode.UNSUPPORTED_RESPONSE_TYPE,
                new UnsupportedResponseType().getErrorCode());
        Assert.assertEquals(ErrorCode.INVALID_SCOPE,
                new InvalidScopeException().getErrorCode());
        Assert.assertEquals(ErrorCode.SERVER_ERROR,
                new ServerErrorException().getErrorCode());
        Assert.assertEquals(ErrorCode.TEMPORARILY_UNAVAILABLE,
                new TemporarilyUnavailableException().getErrorCode());
        Assert.assertEquals(ErrorCode.UNSUPPORTED_GRANT_TYPE,
                new UnsupportedGrantTypeException().getErrorCode());
        Assert.assertEquals(ErrorCode.INVALID_GRANT,
                new InvalidGrantException().getErrorCode());
        Assert.assertEquals(ErrorCode.INVALID_CLIENT,
                new InvalidClientException().getErrorCode());
    }

    /**
     * Assert that we can set redirection URI's.
     */
    @Test
    public void testExpectedRedirectError() {
        URI redirect = UriBuilder.fromPath("http://example.com").build();
        Assert.assertEquals(redirect,
                new InvalidRequestException(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new UnauthorizedClientException(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new AccessDeniedException(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new UnsupportedResponseType(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new InvalidScopeException(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new ServerErrorException(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new TemporarilyUnavailableException(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new UnsupportedGrantTypeException(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new InvalidGrantException(redirect).getRedirect());
        Assert.assertEquals(redirect,
                new InvalidClientException(redirect).getRedirect());
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
