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

/**
 * Unit test for the Abstract OIDC Exception.
 *
 * @author Michael Krotscheck
 */
public final class AbstractOAuthExceptionTest {

    /**
     * Assert this exceptions' identity.
     */
    @Test
    public void testIdentity() {
        AbstractOAuthException e = new TestException("test", "test");
        Assert.assertTrue(e instanceof Throwable);
    }

    /**
     * Assert that the passed values are persisted.
     */
    @Test
    public void testReadProperties() {
        AbstractOAuthException e = new TestException("test1", "test2");
        Assert.assertEquals("test1", e.getError());
        Assert.assertEquals("test2", e.getErrorDescription());
    }

    /**
     * A test exception. Used for... TESTING!
     */
    private static final class TestException extends AbstractOAuthException {

        /**
         * Create a new OAuth error with a specific type and message.
         *
         * @param error            The error type.
         * @param errorDescription Human readable error description.
         */
        TestException(final String error,
                      final String errorDescription) {
            super(error, errorDescription);
        }
    }
}
