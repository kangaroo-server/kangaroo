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

package net.krotscheck.api.oauth.token.exception;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static net.krotscheck.api.oauth.token.exception.TokenException.AccessDeniedException;
import static net.krotscheck.api.oauth.token.exception.TokenException.InvalidRequestException;
import static net.krotscheck.api.oauth.token.exception.TokenException.InvalidScopeException;
import static net.krotscheck.api.oauth.token.exception.TokenException.ServerErrorException;
import static net.krotscheck.api.oauth.token.exception.TokenException.TemporarilyUnavailableException;
import static net.krotscheck.api.oauth.token.exception.TokenException.UnauthorizedClientException;
import static net.krotscheck.api.oauth.token.exception.TokenException.UnsupportedResponseTypeException;

/**
 * Test the exceptions, and their expected default properties.
 *
 * @author Michael Krotscheck
 */
public final class TokenExceptionTest {

    /**
     * Assert that the exception types provided all contain the expected
     * properties.
     */
    @Test
    public void testExpectedExceptionError() {
        Assert.assertEquals("invalid_request",
                new InvalidRequestException().getError());
        Assert.assertEquals("unauthorized_client",
                new UnauthorizedClientException().getError());
        Assert.assertEquals("access_denied",
                new AccessDeniedException().getError());
        Assert.assertEquals("unsupported_response_type",
                new UnsupportedResponseTypeException().getError());
        Assert.assertEquals("invalid_scope",
                new InvalidScopeException().getError());
        Assert.assertEquals("server_error",
                new ServerErrorException().getError());
        Assert.assertEquals("temporarily_unavailable",
                new TemporarilyUnavailableException().getError());
        Assert.assertEquals("invalid_grant",
                new TokenException.InvalidGrantException().getError());
        Assert.assertEquals("invalid_client",
                new TokenException.InvalidClientException().getError());
        Assert.assertEquals("unsupported_grant_type",
                new TokenException.UnsupportedGrantTypeException().getError());
    }

    /**
     * Assert that the class is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = TokenException.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }
}
