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

package net.krotscheck.api.oauth.resource.grant;

import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidGrantException;
import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.api.oauth.resource.TokenResponseEntity;
import net.krotscheck.features.database.entity.Client;
import net.krotscheck.features.database.entity.ClientConfig;
import net.krotscheck.features.database.entity.ClientType;
import net.krotscheck.features.database.entity.OAuthToken;
import net.krotscheck.features.database.entity.OAuthTokenType;
import net.krotscheck.test.DatabaseTest;
import net.krotscheck.test.EnvironmentBuilder;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.TimeZone;
import java.util.UUID;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * These tests ensure coverage on the Refresh token grant type
 * handler, covered in RFC6749.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-6">https://tools.ietf.org/html/rfc6749#section-6</a>
 */
public final class RefreshTokenGrantHandlerTest extends DatabaseTest {

    /**
     * The harness under test.
     */
    private RefreshTokenGrantHandler handler;

    /**
     * Current hibernate session.
     */
    private Session session;

    /**
     * A simple, scoped context.
     */
    private EnvironmentBuilder ownerCredsContext;

    /**
     * A simple, scoped context.
     */
    private EnvironmentBuilder authGrantContext;

    /**
     * A non-refresh-token context.
     */
    private EnvironmentBuilder implicitContext;

    /**
     * A context with no configured scopes.
     */
    private EnvironmentBuilder noScopeContext;

    /**
     * A context with an expired refresh token.
     */
    private EnvironmentBuilder expiredContext;

    /**
     * A context with a refresh token that has no associated auth token.
     */
    private EnvironmentBuilder zombieRefreshContext;

    /**
     * Setup the test.
     */
    @Before
    public void initializeEnvironment() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        handler = new RefreshTokenGrantHandler(getSession());
    }

    /**
     * Set up the test harness data.
     */
    @Before
    public void createTestData() {
        OAuthToken authToken;

        authGrantContext = setupEnvironment()
                .client(ClientType.AuthorizationGrant, true)
                .scope("debug")
                .scope("debug1")
                .bearerToken();
        authToken = authGrantContext.getToken();
        authGrantContext
                .token(OAuthTokenType.Refresh, false, "debug", null, authToken);


        ownerCredsContext = setupEnvironment()
                .client(ClientType.OwnerCredentials, true)
                .scope("debug")
                .scope("debug1")
                .bearerToken();
        authToken = ownerCredsContext.getToken();
        ownerCredsContext
                .token(OAuthTokenType.Refresh, false, "debug", null, authToken);


        noScopeContext = setupEnvironment()
                .client(ClientType.OwnerCredentials, true)
                .bearerToken();
        authToken = noScopeContext.getToken();
        noScopeContext
                .token(OAuthTokenType.Refresh, false, null, null, authToken);


        expiredContext = setupEnvironment()
                .client(ClientType.OwnerCredentials, true)
                .scope("debug")
                .bearerToken();
        authToken = expiredContext.getToken();
        expiredContext
                .token(OAuthTokenType.Refresh, true, "debug", null, authToken);


        zombieRefreshContext = setupEnvironment()
                .client(ClientType.OwnerCredentials, true)
                .scope("debug")
                .refreshToken();


        implicitContext = setupEnvironment()
                .client(ClientType.Implicit, true)
                .scope("debug")
                .bearerToken();
        authToken = implicitContext.getToken();
        implicitContext
                .token(OAuthTokenType.Refresh, false, "debug", null, authToken);

        // The environment builder detaches its data, this reconnects it.
        session = getSession();
        session.refresh(ownerCredsContext.getClient());
        session.refresh(authGrantContext.getClient());
        session.refresh(noScopeContext.getClient());
        session.refresh(implicitContext.getClient());
    }

    /**
     * Test that a valid request works, using the Authorization Grant type.
     */
    @Test
    public void testValidAuthorizationGrant() {
        Client authClient = authGrantContext.getClient();
        OAuthToken refreshToken = authGrantContext.getToken();

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
        OAuthToken newRefresh = session.get(OAuthToken.class,
                token.getRefreshToken());
        Assert.assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn());
        Assert.assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        Assert.assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        Assert.assertNull(session.get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        Assert.assertNull(session.get(OAuthToken.class,
                refreshToken.getId()));
    }

    /**
     * Test that a valid request works, using the Owner Credentials type.
     */
    @Test
    public void testValidOwnerCredentials() {
        Client authClient = ownerCredsContext.getClient();
        OAuthToken refreshToken = ownerCredsContext.getToken();

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
        OAuthToken newRefresh = session.get(OAuthToken.class,
                token.getRefreshToken());
        Assert.assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn());
        Assert.assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        Assert.assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        Assert.assertNull(session.get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        Assert.assertNull(session.get(OAuthToken.class,
                refreshToken.getId()));
    }

    /**
     * Test that an invalid client type fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testInvalidClientType() {
        Client authClient = implicitContext.getClient();
        OAuthToken refreshToken = implicitContext.getToken();

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
        OAuthToken authToken = authGrantContext.getToken().getAuthToken();

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
        Client authClient = expiredContext.getClient();
        OAuthToken authToken = expiredContext.getToken();

        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", authClient.getId().toString());
        testData.putSingle("client_secret", authClient.getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "refresh_token");
        testData.putSingle("refresh_token", authToken.getId().toString());

        handler.handle(authClient, testData);
    }

    /**
     * Assert that we cannot request an invalid scope.
     */
    @Test(expected = InvalidScopeException.class)
    public void testInvalidScope() {
        Client authClient = authGrantContext.getClient();
        OAuthToken refreshToken = authGrantContext.getToken();

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
        OAuthToken refreshToken = authGrantContext.getToken();

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
        OAuthToken refreshToken = authGrantContext.getToken();

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
        OAuthToken newRefresh = session.get(OAuthToken.class,
                token.getRefreshToken());
        Assert.assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn());
        Assert.assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        Assert.assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        Assert.assertNull(session.get(OAuthToken.class,
                refreshToken.getAuthToken().getId()));
        Assert.assertNull(session.get(OAuthToken.class,
                refreshToken.getId()));
    }

    /**
     * Assert that we can refresh with a token that has no associated auth
     * token.
     */
    @Test
    public void testZombieRefresh() {
        Client authClient = zombieRefreshContext.getClient();
        OAuthToken refreshToken = zombieRefreshContext.getToken();

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
        OAuthToken newRefresh = session.get(OAuthToken.class,
                token.getRefreshToken());
        Assert.assertEquals((long) ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT,
                newRefresh.getExpiresIn());
        Assert.assertEquals(OAuthTokenType.Refresh, newRefresh.getTokenType());
        Assert.assertEquals(token.getAccessToken(),
                newRefresh.getAuthToken().getId());

        // Ensure that the previous tokens no longer exist.
        Assert.assertNull(session.get(OAuthToken.class,
                refreshToken.getId()));
    }
}
