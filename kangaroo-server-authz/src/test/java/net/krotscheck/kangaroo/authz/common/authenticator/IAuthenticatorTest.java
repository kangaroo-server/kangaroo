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

package net.krotscheck.kangaroo.authz.common.authenticator;

import net.krotscheck.kangaroo.authz.common.authenticator.exception.MisconfiguredAuthenticatorException;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;

/**
 * Test the default behavior.
 *
 * @author Michael Krotscheck
 */
public final class IAuthenticatorTest {

    /**
     * Test the default "passing" behavior for this interface.
     */
    @Test
    public void testDefaultPassingBehavior() {
        IAuthenticator authenticator = new TestableAuthenticator();

        // null input.
        authenticator.validate(null);

        // Null configuration
        Authenticator auth = new Authenticator();
        auth.setConfiguration(null);
        authenticator.validate(auth);

        // Empty configuration
        auth.setConfiguration(new HashMap<>());
        authenticator.validate(auth);
    }

    /**
     * Test the default failing behavior for this interface.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testDefaultFailingBehavior() {
        IAuthenticator authenticator = new TestableAuthenticator();
        Authenticator auth = new Authenticator();
        auth.getConfiguration().put("foo", "bar");
        authenticator.validate(auth);
    }

    /**
     * The testable authenticator.
     */
    public static final class TestableAuthenticator implements IAuthenticator {

        /**
         * Do nothing, we need to test the default behavior.
         *
         * @param configuration The authenticator configuration.
         * @param callback      The full path to our redirection endpoint.
         * @return null
         */
        @Override
        public Response delegate(final Authenticator configuration,
                                 final URI callback) {
            return null;
        }

        /**
         * Do nothing, we're here to test the default behavior.
         *
         * @param authenticator The authenticator configuration.
         * @param parameters    Parameters for the authenticator, retrieved from
         *                      an appropriate source.
         * @param callback      The full path to our redirection endpoint.
         * @return null
         */
        @Override
        public UserIdentity authenticate(final Authenticator authenticator,
                                         final MultivaluedMap<String, String>
                                                 parameters,
                                         final URI callback) {
            return null;
        }
    }
}
    