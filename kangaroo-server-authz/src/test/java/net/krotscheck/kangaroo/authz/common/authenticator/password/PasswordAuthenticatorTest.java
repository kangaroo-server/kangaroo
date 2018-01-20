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

package net.krotscheck.kangaroo.authz.common.authenticator.password;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.test.TestAuthenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidRequestException;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mockito;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the password authenticator.
 *
 * @author Michael Krotscheck
 */
public final class PasswordAuthenticatorTest extends DatabaseTest {

    /**
     * The environment set up for this test suite.
     */
    private static ApplicationContext context;
    /**
     * Test data loading for this test.
     */
    @ClassRule
    public static final TestRule TEST_DATA_RULE =
            new TestDataResource(HIBERNATE_RESOURCE) {
                /**
                 * Initialize the test data.
                 */
                @Override
                protected void loadTestData(final Session session) {
                    context = ApplicationBuilder.newApplication(session)
                            .client(ClientType.OwnerCredentials)
                            .authenticator(AuthenticatorType.Password)
                            .user()
                            .login("login", "password")
                            .build();
                }
            };

    /**
     * Assert that the test delegate does nothing.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testDelegate() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());

        Authenticator config = new Authenticator();
        URI callback = UriBuilder.fromPath("http://example.com").build();

        Response r = a.delegate(config, callback);
        assertNull(r);
    }

    /**
     * Assert that validate passes with no or null parameters.
     */
    @Test
    public void validateNoParams() {
        try {
            Session session = Mockito.mock(Session.class);
            IAuthenticator a = new TestAuthenticator(session);
            Authenticator config = new Authenticator();
            a.validate(config); // This should NOT throw an exception.

            config.setConfiguration(new HashMap<>());
            a.validate(config); // This should NOT throw an exception.
        } catch (Exception e) {
            assertNull(e);
        }
    }

    /**
     * Assert that validate passes with no or null parameters.
     */
    @Test(expected = KangarooException.class)
    public void validateThrowsWithParams() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("foo", "bar");

        Session session = Mockito.mock(Session.class);
        IAuthenticator a = new PasswordAuthenticator(session);
        Authenticator config = new Authenticator();
        config.setConfiguration(hashMap);

        a.validate(config);
        a.validate(null);
    }

    /**
     * Test that a valid authentication works.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testAuthenticateValid() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "login");
        params.add("password", "password");
        UserIdentity i = a.authenticate(context.getAuthenticator(), params,
                null);
        assertEquals("login", i.getRemoteId());
    }

    /**
     * Assert that trying to authenticate with a null input fails.
     *
     * @throws Exception An authenticator exception.
     */
    @Test(expected = InvalidRequestException.class)
    public void testAuthenticateNullConfig() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        a.authenticate(null, params, null);
    }

    /**
     * Assert that trying to authenticate with a null input fails.
     *
     * @throws Exception An authenticator exception.
     */
    @Test(expected = InvalidRequestException.class)
    public void testAuthenticateNullParams() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        Authenticator config = new Authenticator();
        a.authenticate(config, null, null);
    }

    /**
     * Assert that trying to authenticate with no matching identity fails.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testAuthenticateNoIdentity() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "wrongIdentity");
        params.add("password", "password");
        UserIdentity i = a.authenticate(context.getAuthenticator(), params,
                null);
        assertNull(i);
    }

    /**
     * Assert that trying to authenticate with a wrong password fails.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testAuthenticateWrongPassword() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "login");
        params.add("password", "wrongpassword");
        UserIdentity i = a.authenticate(context.getAuthenticator(), params,
                null);
        assertNull(i);
    }
}
