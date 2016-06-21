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
import net.krotscheck.features.database.entity.ClientType;
import net.krotscheck.features.database.entity.UserIdentity;
import net.krotscheck.test.DatabaseTest;
import net.krotscheck.test.EnvironmentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Unit tests for the password authenticator.
 *
 * @author Michael Krotscheck
 */
public final class PasswordAuthenticatorTest extends DatabaseTest {

    /**
     * Basic testing context.
     */
    private EnvironmentBuilder context;

    /**
     * Setup our testing data.
     */
    @Before
    public void setup() {
        context = setupEnvironment()
                .client(ClientType.OwnerCredentials)
                .authenticator("password")
                .user()
                .login("login", "password");
    }

    /**
     * Assert that the test delegate does nothing.
     */
    @Test
    public void testDelegate() {
        IAuthenticator a = new PasswordAuthenticator(getSession());

        Authenticator config = new Authenticator();
        URI callback = UriBuilder.fromPath("http://example.com").build();

        Response r = a.delegate(config, callback);
        Assert.assertNull(r);
    }

    /**
     * Test that a valid authentication works.
     */
    @Test
    public void testAuthenticateValid() {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "login");
        params.add("password", "password");
        UserIdentity i = a.authenticate(context.getAuthenticator(), params);
        Assert.assertEquals(context.getUserIdentity(), i);
    }

    /**
     * Assert that trying to authenticate with a null input fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testAuthenticateNullConfig() {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        a.authenticate(null, params);
    }

    /**
     * Assert that trying to authenticate with a null input fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testAuthenticateNullParams() {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        Authenticator config = new Authenticator();
        a.authenticate(config, null);
    }

    /**
     * Assert that trying to authenticate with no matching identity fails.
     */
    @Test
    public void testAuthenticateNoIdentity() {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "wrongIdentity");
        params.add("password", "password");
        UserIdentity i = a.authenticate(context.getAuthenticator(), params);
        Assert.assertNull(i);
    }

    /**
     * Assert that trying to authenticate with a wrong password fails.
     */
    @Test
    public void testAuthenticateWrongPassword() {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "login");
        params.add("password", "wrongpassword");
        UserIdentity i = a.authenticate(context.getAuthenticator(), params);
        Assert.assertNull(i);
    }
}
