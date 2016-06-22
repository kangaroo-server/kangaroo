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

package net.krotscheck.api.oauth.authenticator;

import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidRequestException;
import net.krotscheck.features.database.entity.Authenticator;
import net.krotscheck.features.database.entity.UserIdentity;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Test the utility methods in the Abstract Authenticator.
 */
public final class AbstractAuthenticatorTest {

    /**
     * Assert that testOne will always return one value.
     */
    @Test
    public void testGetOne() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("foo", "bar");

        TestAuthenticator t = new TestAuthenticator();
        String result = t.getOne(params, "foo");
        Assert.assertEquals("bar", result);
    }

    /**
     * Assert that testOne throws an exception if no value exists to retrieve.
     */
    @Test(expected = InvalidRequestException.class)
    public void testGetOneNoValue() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        TestAuthenticator t = new TestAuthenticator();
        t.getOne(params, "does_not_exist");
    }

    /**
     * Assert that testOne throws an exception if more than one value exists.
     */
    @Test(expected = InvalidRequestException.class)
    public void testGetOneMultiResult() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("foo", "one");
        params.add("foo", "two");

        TestAuthenticator t = new TestAuthenticator();
        t.getOne(params, "foo");
    }


    private static final class TestAuthenticator extends AbstractAuthenticator {

        /**
         * Delegate an authentication request to a third party authentication
         * provider, such as Google, Facebook, etc.
         *
         * @param configuration The authenticator configuration.
         * @param callback      The redirect, on this server, where the
         *                      response
         *                      should go.
         * @return An HTTP response, redirecting the client to the next step.
         */
        @Override
        public Response delegate(final Authenticator configuration,
                                 final URI callback) {
            return null; // Do nothing
        }

        /**
         * Authenticate and/or create a user identity for a specific client,
         * given
         * the URI from an authentication delegate.
         *
         * @param configuration The authenticator configuration.
         * @param parameters    Parameters for the authenticator, retrieved
         *                      from
         *                      an appropriate source.
         * @return A user identity.
         */
        @Override
        public UserIdentity authenticate(final Authenticator configuration,
                                         final MultivaluedMap<String, String>
                                                 parameters) {
            return null;
        }
    }
}