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

package net.krotscheck.kangaroo.authz.oauth2.exception;

import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidClientException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidRequestException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidScopeException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.ServerErrorException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.TemporarilyUnavailableException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.UnauthorizedClientException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.UnsupportedGrantTypeException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.UnsupportedResponseTypeException;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Data validation tests for the errors defined in RFC6749.
 *
 * @author Michael Krotscheck
 */
public final class RFC6749Test {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = RFC6749.class.getDeclaredConstructor();
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
    public void testErrors() throws Exception {

        validateCodeInException(
                "access_denied",
                AccessDeniedException.class);
        validateCodeInException(
                "invalid_client",
                InvalidClientException.class);
        validateCodeInException(
                "invalid_grant",
                InvalidGrantException.class);
        validateCodeInException(
                "invalid_request",
                InvalidRequestException.class);
        validateCodeInException(
                "invalid_scope",
                InvalidScopeException.class);
        validateCodeInException(
                "server_error",
                ServerErrorException.class);
        validateCodeInException(
                "temporarily_unavailable",
                TemporarilyUnavailableException.class);
        validateCodeInException(
                "unauthorized_client",
                UnauthorizedClientException.class);
        validateCodeInException(
                "unsupported_grant_type",
                UnsupportedGrantTypeException.class);
        validateCodeInException(
                "unsupported_response_type",
                UnsupportedResponseTypeException.class);
    }

    /**
     * Perform various error code validations on each exception.
     *
     * @param expectedCode The expected string code.
     * @param errorClass   The class of error.
     * @throws Exception Should not be thrown.
     */
    private void validateCodeInException(final String expectedCode,
                                         final Class<? extends
                                                 KangarooException> errorClass)
            throws Exception {

        // Try creating a regular version and make sure it has valid values.
        KangarooException e = errorClass.newInstance();
        // We assume that KangarooException is well tested.
        Assert.assertEquals(expectedCode, e.getCode().getError());
    }
}
