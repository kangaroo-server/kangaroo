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

package net.krotscheck.kangaroo.authz.oauth2.resource.token;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientConfig;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidScopeException;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * These tests ensure coverage on the Refresh token token type
 * handler, covered in RFC6749.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-6">https://tools.ietf.org/html/rfc6749#section-6</a>
 */
public final class RefreshTokenGrantHandlerTest extends DatabaseTest {

    /**
     * A simple, scoped context.
     */
    private static ApplicationContext ownerCredsContext;
    /**
     * A simple, scoped context.
     */
    private static ApplicationContext authGrantContext;
    /**
     * A non-refresh-token context.
     */
    private static ApplicationContext implicitContext;
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
                    authGrantContext = ApplicationBuilder
                            .newApplication(session)
                            .client(ClientType.AuthorizationGrant, true)
                            .authenticator(AuthenticatorType.Test)
                            .scope("debug")
                            .scope("debug1")
                            .role("test", new String[]{"debug", "debug1"})
                            .user()
                            .identity("remote_identity")
                            .build();

                    ownerCredsContext = ApplicationBuilder
                            .newApplication(session)
                            .client(ClientType.OwnerCredentials, true)
                            .authenticator(AuthenticatorType.Test)
                            .scope("debug")
                            .scope("debug1")
                            .role("test", new String[]{"debug", "debug1"})
                            .user()
                            .identity("remote_identity")
                            .build();

                    implicitContext = ApplicationBuilder
                            .newApplication(session)
                            .client(ClientType.Implicit, true)
                            .authenticator(AuthenticatorType.Test)
                            .scope("debug")
                            .role("test", new String[]{"debug"})
                            .build();
                }
            };
    /**
     * The harness under test.
     */
    private RefreshTokenGrantHandler handler;

    /**
     * Setup the test.
     */
    @Before
    public void initializeEnvironment() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        handler = new RefreshTokenGrantHandler(getSession());
    }

    /**
     * Test that a valid request works, using the Authorization Grant type.
     */
    @Test
    public void testValidAuthorizationGrant() {
        Client authClient = authGrantContext.getClient();
        OAuthToken refreshToken = authGrantContext.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build()
                .getToken();

        TokenResponseEntity token = handler.handle(authClient, "debug",
                null, refreshToken.getId());
        assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertEquals("debug", token.getScope());

        // Resolve the granted refresh token
        OAuthToken newRefresh = getSession().get(OAuthToken.class,
                token.getRefreshToken());
        assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn().longValue());
        assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        assertNull(getSession().get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        assertNull(getSession().get(OAuthToken.class,
                refreshToken.getId()));
    }

    /**
     * Test that a valid request works, using the Owner Credentials type.
     */
    @Test
    public void testValidOwnerCredentials() {
        Client authClient = ownerCredsContext.getClient();
        OAuthToken refreshToken = ownerCredsContext.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build()
                .getToken();

        TokenResponseEntity token = handler.handle(authClient, "debug",
                null, refreshToken.getId());
        assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertEquals("debug", token.getScope());

        // Resolve the granted refresh token
        OAuthToken newRefresh = getSession().get(OAuthToken.class,
                token.getRefreshToken());
        assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn().longValue());
        assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        assertNull(getSession().get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        assertNull(getSession().get(OAuthToken.class,
                refreshToken.getId()));
    }

    /**
     * Test that an invalid client type fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testInvalidClientType() {
        Client authClient = implicitContext.getClient();
        OAuthToken refreshToken = implicitContext.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build()
                .getToken();

        handler.handle(authClient, "debug",
                null, refreshToken.getId());
    }

    /**
     * Assert that passing an invalid refresh token fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testInvalidRefreshToken() {
        Client authClient = authGrantContext.getClient();

        handler.handle(authClient, "debug",
                null, IdUtil.next());
    }

    /**
     * Assert that passing no refresh token fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testNullRefreshToken() {
        Client authClient = authGrantContext.getClient();

        handler.handle(authClient, "debug",
                null, null);
    }

    /**
     * Assert that passing the ID to a non-refresh-token fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testNotARefreshToken() {
        Client authClient = authGrantContext.getClient();
        OAuthToken authToken = authGrantContext.getBuilder()
                .bearerToken()
                .build()
                .getToken();

        handler.handle(authClient, "debug",
                null, authToken.getId());
    }

    /**
     * Assert that an expired refresh token fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testExpiredToken() {
        Client authClient = ownerCredsContext.getClient();
        OAuthToken refreshToken = ownerCredsContext.getBuilder()
                .bearerToken()
                .token(OAuthTokenType.Refresh, true, "debug", null, null)
                .build()
                .getToken();

        handler.handle(authClient, "debug",
                null, refreshToken.getId());
    }

    /**
     * Assert that we cannot request an invalid scope.
     */
    @Test(expected = InvalidScopeException.class)
    public void testInvalidScope() {
        Client authClient = authGrantContext.getClient();
        OAuthToken refreshToken = authGrantContext.getBuilder()
                .bearerToken()
                .refreshToken()
                .build()
                .getToken();

        handler.handle(authClient, "debug invalid",
                null, refreshToken.getId());
    }

    /**
     * Assert that we cannot escalate scope.
     */
    @Test(expected = InvalidScopeException.class)
    public void testEscalateScope() {
        Client authClient = authGrantContext.getClient();
        OAuthToken refreshToken = authGrantContext.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build()
                .getToken();

        handler.handle(authClient, "debug debug1",
                null, refreshToken.getId());
    }

    /**
     * Assert that we can de-escalate scope.
     */
    @Test
    public void testDeescalateScope() {
        Client authClient = authGrantContext.getClient();
        OAuthToken refreshToken = authGrantContext.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build()
                .getToken();

        TokenResponseEntity token = handler.handle(authClient, "",
                null, refreshToken.getId());
        assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertNull(token.getScope());

        // Resolve the granted refresh token
        OAuthToken newRefresh = getSession().get(OAuthToken.class,
                token.getRefreshToken());
        assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn().longValue());
        assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        assertNull(getSession().get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        assertNull(getSession().get(OAuthToken.class,
                refreshToken.getId()));
    }

    /**
     * Assert that we can refresh with a token that has no associated auth
     * token.
     */
    @Test
    public void testZombieRefresh() {
        Client authClient = authGrantContext.getClient();
        OAuthToken refreshToken = authGrantContext.getBuilder()
                .token(OAuthTokenType.Refresh, false, "debug", null, null)
                .build()
                .getToken();

        TokenResponseEntity token = handler.handle(authClient, "",
                null, refreshToken.getId());
        assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertNull(token.getScope());

        // Resolve the granted refresh token
        OAuthToken newRefresh = getSession().get(OAuthToken.class,
                token.getRefreshToken());
        assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn().longValue());
        assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        assertNull(getSession().get(OAuthToken.class,
                refreshToken.getId()));
    }
}
