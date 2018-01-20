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

package net.krotscheck.kangaroo.authz.oauth2.rfc7662;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.OAuthAPI;
import net.krotscheck.kangaroo.authz.oauth2.resource.IntrospectionResponseEntity;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import net.krotscheck.kangaroo.test.jersey.SingletonTestContainerFactory;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import net.krotscheck.kangaroo.util.StringUtil;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static net.krotscheck.kangaroo.util.HttpUtil.authHeaderBasic;
import static net.krotscheck.kangaroo.util.HttpUtil.authHeaderBearer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test proper behavior on the token introspection endpoint.
 *
 * @author Michael Krotscheck
 */
public final class TokenIntrospectionTest extends ContainerTest {

    /**
     * The test context for a regular application.
     */
    private static ApplicationContext context;

    /**
     * Authorization header for the main context.
     */
    private static String contextAuthHeader;

    /**
     * The test context for a peer application.
     */
    private static ApplicationContext otherContext;
    /**
     * Authorization header for the peer context.
     */
    private static String otherContextAuthHeader;
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
                    context = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .scope("debug1")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.AuthorizationGrant, true)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://www.example.com/")
                            .user()
                            .identity()
                            .claim("one", "claim")
                            .claim("two", "claim")
                            .build();
                    otherContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .scope("debug1")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.AuthorizationGrant, true)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://www.example.com/")
                            .user()
                            .identity()
                            .claim("red", "claim")
                            .claim("blue", "claim")
                            .build();

                    Client contextClient = context.getClient();
                    contextAuthHeader =
                            authHeaderBasic(contextClient.getId(),
                                    contextClient.getClientSecret());

                    Client otherClient = otherContext.getClient();
                    otherContextAuthHeader =
                            authHeaderBasic(otherClient.getId(),
                                    otherClient.getClientSecret());

                }
            };
    /**
     * Test container factory.
     */
    private SingletonTestContainerFactory testContainerFactory;

    /**
     * The current running test application.
     */
    private ResourceConfig testApplication;

    /**
     * This method overrides the underlying default test container provider,
     * with one that provides a singleton instance. This allows us to
     * circumvent the often expensive initialization routines that come from
     * bootstrapping our services.
     *
     * @return an instance of {@link TestContainerFactory} class.
     * @throws TestContainerException if the initialization of
     *                                {@link TestContainerFactory} instance
     *                                is not successful.
     */
    protected TestContainerFactory getTestContainerFactory()
            throws TestContainerException {
        if (this.testContainerFactory == null) {
            this.testContainerFactory =
                    new SingletonTestContainerFactory(
                            super.getTestContainerFactory(),
                            this.getClass());
        }
        return testContainerFactory;
    }

    /**
     * Create the application under test.
     *
     * @return A configured api servlet.
     */
    @Override
    protected ResourceConfig createApplication() {
        if (testApplication == null) {
            testApplication = new OAuthAPI();
        }
        return testApplication;
    }

    /**
     * Convert a map into an HTML form payload.
     *
     * @param values The values to map.
     * @return The payload.
     */
    private Entity buildEntity(final Map<String, String> values) {
        Form f = new Form();
        values.forEach(f::param);
        return Entity.entity(f, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }

    /**
     * Assert that the provided response is a valid token introspection
     * response for the provided token.
     *
     * @param r The response.
     */
    private void assertInactiveIntrospectionResponse(final Response r) {
        assertEquals(200, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, r.getMediaType().toString());

        IntrospectionResponseEntity entity =
                r.readEntity(IntrospectionResponseEntity.class);
        assertNotNull(entity);

        assertFalse(entity.isActive());

        assertNull(entity.getClientId());
        assertNull(entity.getTokenType());
        assertNull(entity.getIat());
        assertNull(entity.getJti());
        assertNull(entity.getNbf());
        assertNull(entity.getAud());
        assertNull(entity.getIss());
        assertNull(entity.getScope());
        assertNull(entity.getSub());
    }

    /**
     * Assert that the provided response is a valid token introspection
     * response for the provided token.
     *
     * @param r     The response.
     * @param token The bearer token.
     */
    private void assertSuccessfulIntrospectionResponse(final Response r,
                                                       final OAuthToken token) {
        assertEquals(200, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, r.getMediaType().toString());

        IntrospectionResponseEntity entity =
                r.readEntity(IntrospectionResponseEntity.class);
        assertNotNull(entity);

        // Focus on per-second calculations.
        Calendar created = (Calendar) token.getCreatedDate().clone();
        created.set(Calendar.MILLISECOND, 0);

        // Checks valid for all token types.
        assertEquals(token.getClient().getId(), entity.getClientId());
        assertEquals(token.getTokenType(), entity.getTokenType());
        assertEquals(created, entity.getIat());
        assertEquals(!token.isExpired(), entity.isActive());
        assertEquals(token.getId(), entity.getJti());
        assertEquals(created, entity.getNbf());
        assertEquals(token.getClient().getApplication().getId(),
                entity.getAud());

        Calendar expires = (Calendar) created.clone();
        expires.add(Calendar.SECOND, token.getExpiresIn().intValue());
        assertEquals(expires, entity.getExp());

        assertEquals("localhost", entity.getIss());

        // Create the scope list and compare.
        String scopes = StringUtil.sameOrDefault(entity.getScope(), "");
        List<String> scopeList = Arrays.asList(scopes.split(" "));
        List<String> tokenScopes = new ArrayList<>(token.getScopes().keySet());
        assertEquals(scopeList, tokenScopes);

        // Relevant to different client types.
        if (token.getClient().getType().equals(ClientType.ClientCredentials)) {
            assertNull(token.getIdentity());
            assertEquals(token.getClient().getId(), entity.getSub());
        } else {
            assertNotNull(token.getIdentity());
            assertEquals(token.getIdentity().getRemoteId(),
                    entity.getUsername());
            assertEquals(token.getIdentity().getUser().getId(),
                    entity.getSub());
        }
    }

    /**
     * Assert that we can perform a valid token self-introspection request.
     */
    @Test
    public void testTokenIntrospectSelf() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(bearerToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertSuccessfulIntrospectionResponse(r, bearerToken);
    }

    /**
     * Assert that a token cannot introspect another token (Use the admin api
     * for this).
     */
    @Test
    public void testTokenIntrospectByOtherToken() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        ApplicationContext testContext2 = context.getBuilder()
                .bearerToken()
                .build();
        OAuthToken bearerToken2 = testContext2.getToken();

        String header = authHeaderBearer(bearerToken2.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(bearerToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that an authorization code cannot be introspected by a bearer
     * token.
     */
    @Test
    public void testAuthCodeIntrospect() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        ApplicationContext testContext2 = context.getBuilder()
                .authToken()
                .build();
        OAuthToken authToken = testContext2.getToken();

        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(authToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that an authorization code cannot be used to introspect itself.
     */
    @Test
    public void testAuthCodeIntrospectSelf() {
        ApplicationContext testContext = context.getBuilder()
                .authToken()
                .build();
        OAuthToken authToken = testContext.getToken();

        String header = authHeaderBearer(authToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(authToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, Status.UNAUTHORIZED, "access_denied");
    }

    /**
     * Assert that a refresh token cannot be introspected by a bearer token.
     */
    @Test
    public void testRefreshTokenIntrospect() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        ApplicationContext testContext2 = context.getBuilder()
                .refreshToken()
                .build();
        OAuthToken refreshToken = testContext2.getToken();

        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(refreshToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that a refresh token cannot be used to introspect itself.
     */
    @Test
    public void testRefreshTokenIntrospectSelf() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build();
        OAuthToken refreshToken = testContext.getToken();

        String header = authHeaderBearer(refreshToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(refreshToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, Status.UNAUTHORIZED, "access_denied");
    }

    /**
     * Assert that an expired bearer token can be introspected.
     */
    @Test
    public void testExpiredBearerTokenIntrospect() {
        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.ClientCredentials, true)
                .token(OAuthTokenType.Bearer, true, "debug", null, null)
                .build();
        OAuthToken targetToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(targetToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, contextAuthHeader)
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that an expired authorization token can be introspected.
     */
    @Test
    public void testExpiredAuthorizationTokenIntrospect() {
        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.ClientCredentials, true)
                .token(OAuthTokenType.Authorization, true, "debug", null, null)
                .build();
        OAuthToken targetToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(targetToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, contextAuthHeader)
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that an expired refresh token can be introspected.
     */
    @Test
    public void testExpiredRefreshTokenIntrospect() {
        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.ClientCredentials, true)
                .token(OAuthTokenType.Refresh, true, "debug", null, null)
                .build();
        OAuthToken targetToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(targetToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, contextAuthHeader)
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that a nonexistent refresh token can be introspected.
     */
    @Test
    public void testNonexistentTokenIntrospect() {
        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(IdUtil.next()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, contextAuthHeader)
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that a nonexistent refresh token can be introspected.
     */
    @Test
    public void testMalformedTokenIntrospect() {
        Map<String, String> values = new HashMap<>();
        values.put("token", "malformed_token");

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, contextAuthHeader)
                .post(buildEntity(values));

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that a token can be introspected by a client using header auth.
     */
    @Test
    public void testBearerTokenIntrospectByClientInHeader() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken targetToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(targetToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, contextAuthHeader)
                .post(buildEntity(values));

        assertSuccessfulIntrospectionResponse(r, targetToken);
    }

    /**
     * Assert that a token can be introspected by a client using form body auth.
     */
    @Test
    public void testRefreshTokenIntrospectByClientInBody() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build();
        OAuthToken targetToken = testContext.getToken();
        Client queryClient = testContext.getClient();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(targetToken.getId()));
        values.put("client_id", IdUtil.toString(queryClient.getId()));
        values.put("client_secret", queryClient.getClientSecret());

        Response r = target("/introspect")
                .request()
                .post(buildEntity(values));

        assertSuccessfulIntrospectionResponse(r, targetToken);
    }

    /**
     * Assert that a token can be introspected by a different client on the
     * same application, in form body auth.
     */
    @Test
    public void testTokenIntrospectByApplicationPeerClientInBody() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .client(ClientType.ClientCredentials, true)
                .build();
        OAuthToken targetToken = testContext.getToken();
        Client queryClient = testContext.getClient();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(targetToken.getId()));
        values.put("client_id", IdUtil.toString(queryClient.getId()));
        values.put("client_secret", queryClient.getClientSecret());

        Response r = target("/introspect")
                .request()
                .post(buildEntity(values));

        assertSuccessfulIntrospectionResponse(r, targetToken);
    }

    /**
     * Assert that a token can be introspected by a different client on the
     * same application, in header auth.
     */
    @Test
    public void testTokenIntrospectByApplicationPeerClientInHeader() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .client(ClientType.ClientCredentials, true)
                .build();
        OAuthToken targetToken = testContext.getToken();
        Client queryClient = testContext.getClient();
        String header = authHeaderBasic(
                queryClient.getId(), queryClient.getClientSecret());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(targetToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertSuccessfulIntrospectionResponse(r, targetToken);
    }

    /**
     * Assert that a token cannot be accessed by a client on another
     * application, in body auth.
     */
    @Test
    public void testTokenIntrospectFromOtherApplicationInBody() {
        ApplicationContext tokenContext = context.getBuilder()
                .bearerToken("debug")
                .build();

        OAuthToken bearerToken = tokenContext.getToken();
        Client queryClient = otherContext.getClient();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(bearerToken.getId()));
        values.put("client_id", IdUtil.toString(queryClient.getId()));
        values.put("client_secret", queryClient.getClientSecret());

        Response r = target("/introspect")
                .request()
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that a token cannot be accessed by a client on another
     * application, in form header auth.
     */
    @Test
    public void testTokenIntrospectFromOtherApplicationInHeader() {
        ApplicationContext tokenContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = tokenContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(bearerToken.getId()));

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, otherContextAuthHeader)
                .post(buildEntity(values));

        assertInactiveIntrospectionResponse(r);
    }

    /**
     * Assert that trying to authorize using both the header and the body
     * method passes, assuming they're identical.
     */
    @Test
    public void testDualAuthPasses() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        Client queryClient = testContext.getClient();
        String header = authHeaderBasic(
                queryClient.getId(), queryClient.getClientSecret());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(bearerToken.getId()));
        values.put("client_id", IdUtil.toString(queryClient.getId()));
        values.put("client_secret", queryClient.getClientSecret());

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertSuccessfulIntrospectionResponse(r, bearerToken);
    }

    /**
     * Assert that trying to authorize using a bearer token and the form body
     * fails.
     */
    @Test
    public void testTokenAuthWithBodyClientAuth() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        Client queryClient = testContext.getClient();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(bearerToken.getId()));
        values.put("client_id", IdUtil.toString(queryClient.getId()));
        values.put("client_secret", queryClient.getClientSecret());

        Response r = target("/introspect")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, Status.UNAUTHORIZED, "access_denied");
    }
}
