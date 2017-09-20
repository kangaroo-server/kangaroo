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
import net.krotscheck.kangaroo.authz.common.authenticator.password.PasswordAuthenticator.Binder;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidRequestException;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static junit.framework.TestCase.assertNotNull;

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
        Assert.assertNull(r);
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
        UserIdentity i = a.authenticate(context.getAuthenticator(), params);
        Assert.assertEquals("login", i.getRemoteId());
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
        a.authenticate(null, params);
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
        a.authenticate(config, null);
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
        UserIdentity i = a.authenticate(context.getAuthenticator(), params);
        Assert.assertNull(i);
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
        UserIdentity i = a.authenticate(context.getAuthenticator(), params);
        Assert.assertNull(i);
    }
}
