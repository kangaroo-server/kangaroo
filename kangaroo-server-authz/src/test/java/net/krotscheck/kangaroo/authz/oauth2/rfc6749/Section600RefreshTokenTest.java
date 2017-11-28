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

package net.krotscheck.kangaroo.authz.oauth2.rfc6749;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * These tests validate the refresh token flow.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-6">https://tools.ietf.org/html/rfc6749#section-6</a>
 */
public final class Section600RefreshTokenTest
        extends AbstractRFC6749Test {

    /**
     * The test context for a public application.
     */
    private static ApplicationContext context;
    /**
     * The test context for a private application.
     */
    private static ApplicationContext authContext;
    /**
     * The auth header string for each test.
     */
    private static String authHeader;
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
                            .scope("debug")
                            .scope("debug2")
                            .role("test", new String[]{"debug", "debug2"})
                            .client(ClientType.AuthorizationGrant)
                            .authenticator(AuthenticatorType.Test)
                            .user()
                            .identity("test_identity_1")
                            .build();

                    authContext = ApplicationBuilder.newApplication(session)
                            .scope("debug")
                            .scope("debug2")
                            .role("test", new String[]{"debug", "debug2"})
                            .client(ClientType.OwnerCredentials, true)
                            .authenticator(AuthenticatorType.Test)
                            .user()
                            .identity("test_identity_2")
                            .build();

                    authHeader = HttpUtil.authHeaderBasic(
                            authContext.getClient().getId(),
                            authContext.getClient().getClientSecret());
                }
            };

    /**
     * Assert that a simple refresh request works.
     */
    @Test
    public void testTokenSimpleRequest() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, false);
        assertNull(entity.getScope());
    }

    /**
     * Assert that an authenticated refresh request works, via body
     * authentication.
     */
    @Test
    public void testAuthViaBodyRequest() {
        ApplicationContext testContext = authContext.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("client_secret",
                testContext.getClient().getClientSecret());
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, false);
        assertNull(entity.getScope());
    }

    /**
     * Assert that an authenticated refresh request with bad credentials fails.
     */
    @Test
    public void testWrongAuthViaBodyRequest() {
        ApplicationContext testContext = authContext.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("client_secret", "wrong_secret");
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("access_denied", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an authenticated refresh request works, via header
     * authentication.
     */
    @Test
    public void testAuthViaHeaderRequest() {
        ApplicationContext testContext = authContext.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, false);
        assertNull(entity.getScope());
    }

    /**
     * Assert that an authenticated refresh request with bad credentials fails.
     */
    @Test
    public void testWrongAuthViaHeaderRequest() {
        ApplicationContext testContext = authContext.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        String badHeader = HttpUtil.authHeaderBasic(
                testContext.getClient().getId(), "badsecret");

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", badHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("access_denied", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that a user may only use one auth method.
     */
    @Test
    public void testOnlyOneAuthMethod() {
        ApplicationContext testContext = authContext.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("client_secret",
                testContext.getClient().getClientSecret());
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that a client ID mismatch between header and body is not
     * permitted.
     */
    @Test
    public void testMismatchedClient() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that omitting a client id is not permitted.
     */
    @Test
    public void testNoClient() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an invalid refresh token does not work.
     */
    @Test
    public void testInvalidRefreshToken() {
        Client c = context.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(c.getId()));
        f.param("refresh_token", "invalid_token");
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("not_found", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that an expired refresh token is not permitted.
     */
    @Test
    public void testExpiredRefreshToken() {
        ApplicationContext bridgeContext = context.getBuilder()
                .bearerToken()
                .build();
        ApplicationContext testContext = bridgeContext.getBuilder()
                .token(OAuthTokenType.Bearer, true, null, null,
                        bridgeContext.getToken())
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_grant", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that a used refresh token is invalidated once a new one is issued.
     */
    @Test
    public void testRefreshInvalidatedOnIssue() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, false);
        assertNull(entity.getScope());

        // Now do the whole thing again.
        Response r2 = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r2.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r2.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity2 = r2.readEntity(ErrorResponse.class);
        assertEquals("invalid_grant", entity2.getError());
        assertNotNull(entity2.getErrorDescription());
    }

    /**
     * Test that a refreshed token receives the scope of the original access
     * token.
     */
    @Test
    public void testScopePersistedOnRefresh() {
        ApplicationContext bridgeContext = context.getBuilder()
                .bearerToken()
                .build();
        ApplicationContext testContext = bridgeContext.getBuilder()
                .token(OAuthTokenType.Refresh, false, "debug debug2", null,
                        bridgeContext.getToken())
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");
        f.param("scope", "debug debug2");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, false);
        assertEquals("debug debug2", entity.getScope());
    }

    /**
     * Test that a refreshed token may request a subset of the authorization
     * scope.
     */
    @Test
    public void testScopeSubsetSelected() {
        ApplicationContext bridgeContext = context.getBuilder()
                .bearerToken()
                .build();
        ApplicationContext testContext = bridgeContext.getBuilder()
                .token(OAuthTokenType.Refresh, false, "debug debug2", null,
                        bridgeContext.getToken())
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");
        f.param("scope", "debug2");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, false);
        assertEquals("debug2", entity.getScope());
    }

    /**
     * Test that a refreshed token may not escalate its scope in a refresh
     * request.
     */
    @Test
    public void testScopeEscalationFails() {
        ApplicationContext testContext = context.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("refresh_token",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "refresh_token");
        f.param("scope", "debug debug2");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_scope", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }
}
