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
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.TimeZone;
import java.util.UUID;

/**
 * These tests ensure coverage on the Refresh token token type
 * handler, covered in RFC6749.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-6">https://tools.ietf.org/html/rfc6749#section-6</a>
 */
public final class RefreshTokenGrantHandlerTest extends DatabaseTest {

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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", refreshToken.getId().toString());

        TokenResponseEntity token = handler.handle(authClient, testData);
        Assert.assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        Assert.assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        Assert.assertNotNull(token.getAccessToken());
        Assert.assertNotNull(token.getRefreshToken());
        Assert.assertEquals("debug", token.getScope());

        // Resolve the granted refresh token
        OAuthToken newRefresh = getSession().get(OAuthToken.class,
                token.getRefreshToken());
        Assert.assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn().longValue());
        Assert.assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        Assert.assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        Assert.assertNull(getSession().get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        Assert.assertNull(getSession().get(OAuthToken.class,
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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", refreshToken.getId().toString());

        TokenResponseEntity token = handler.handle(authClient, testData);
        Assert.assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        Assert.assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        Assert.assertNotNull(token.getAccessToken());
        Assert.assertNotNull(token.getRefreshToken());
        Assert.assertEquals("debug", token.getScope());

        // Resolve the granted refresh token
        OAuthToken newRefresh = getSession().get(OAuthToken.class,
                token.getRefreshToken());
        Assert.assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn().longValue());
        Assert.assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        Assert.assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        Assert.assertNull(getSession().get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        Assert.assertNull(getSession().get(OAuthToken.class,
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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", refreshToken.getId().toString());

        handler.handle(authClient, testData);
    }

    /**
     * Assert that passing a non-conforming refresh token fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testMalformedRefreshToken() {
        Client authClient = authGrantContext.getClient();

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", "not_a_uuid");

        handler.handle(authClient, testData);
    }

    /**
     * Assert that passing an invalid refresh token fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testInvalidRefreshToken() {
        Client authClient = authGrantContext.getClient();

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", UUID.randomUUID().toString());

        handler.handle(authClient, testData);
    }

    /**
     * Assert that passing no refresh token fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testNullRefreshToken() {
        Client authClient = authGrantContext.getClient();

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");

        handler.handle(authClient, testData);
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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", authToken.getId().toString());

        handler.handle(authClient, testData);
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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", refreshToken.getId().toString());

        handler.handle(authClient, testData);
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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug invalid");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", refreshToken.getId().toString());

        handler.handle(authClient, testData);
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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug debug1");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", refreshToken.getId().toString());

        handler.handle(authClient, testData);
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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", refreshToken.getId().toString());

        TokenResponseEntity token = handler.handle(authClient, testData);
        Assert.assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        Assert.assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        Assert.assertNotNull(token.getAccessToken());
        Assert.assertNotNull(token.getRefreshToken());
        Assert.assertNull(token.getScope());

        // Resolve the granted refresh token
        OAuthToken newRefresh = getSession().get(OAuthToken.class,
                token.getRefreshToken());
        Assert.assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn().longValue());
        Assert.assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        Assert.assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        Assert.assertNull(getSession().get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        Assert.assertNull(getSession().get(OAuthToken.class,
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

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", refreshToken.getId().toString());

        TokenResponseEntity token = handler.handle(authClient, testData);
        Assert.assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        Assert.assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        Assert.assertNotNull(token.getAccessToken());
        Assert.assertNotNull(token.getRefreshToken());
        Assert.assertNull(token.getScope());

        // Resolve the granted refresh token
        OAuthToken newRefresh = getSession().get(OAuthToken.class,
                token.getRefreshToken());
        Assert.assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn().longValue());
        Assert.assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        Assert.assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        Assert.assertNull(getSession().get(OAuthToken.class,
                refreshToken.getId()));
    }
}
