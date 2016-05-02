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

import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.AccessDeniedException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.AccountSelectionRequiredException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.ConsentRequiredException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.InteractionRequiredException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.InvalidRequestException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.InvalidRequestObjectException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.InvalidRequestUriException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.InvalidScopeException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.LoginRequiredException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.RegistrationNotSupportedException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.RequestNotSupportedException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.RequestUriNotSupportedException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.ServerErrorException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.TemporarilyUnavailableException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.UnauthorizedClientException;
import static net.krotscheck.api.oauth.exception.exception.AuthenticationException.UnsupportedResponseType;

/**
 * Test the exceptions, and their expected default properties.
 *
 * @author Michael Krotscheck
 */
public final class AuthenticationExceptionTest {

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
                new UnsupportedResponseType().getError());
        Assert.assertEquals("invalid_scope",
                new InvalidScopeException().getError());
        Assert.assertEquals("server_error",
                new ServerErrorException().getError());
        Assert.assertEquals("temporarily_unavailable",
                new TemporarilyUnavailableException().getError());
        Assert.assertEquals("interaction_required",
                new InteractionRequiredException().getError());
        Assert.assertEquals("login_required",
                new LoginRequiredException().getError());
        Assert.assertEquals("account_selection_required",
                new AccountSelectionRequiredException().getError());
        Assert.assertEquals("consent_required",
                new ConsentRequiredException().getError());
        Assert.assertEquals("invalid_request_uri",
                new InvalidRequestUriException().getError());
        Assert.assertEquals("invalid_request_object",
                new InvalidRequestObjectException().getError());
        Assert.assertEquals("request_not_supported",
                new RequestNotSupportedException().getError());
        Assert.assertEquals("request_uri_not_supported",
                new RequestUriNotSupportedException().getError());
        Assert.assertEquals("registration_not_supported",
                new RegistrationNotSupportedException().getError());
    }

    /**
     * Assert that the class is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = AuthenticationException.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }
}
