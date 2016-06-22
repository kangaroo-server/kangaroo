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

package net.krotscheck.api.oauth.rfc6749;

import net.krotscheck.api.oauth.resource.TokenResponseEntity;
import net.krotscheck.features.database.entity.Client;
import net.krotscheck.features.database.entity.ClientConfig;
import net.krotscheck.features.database.entity.ClientType;
import net.krotscheck.features.database.entity.OAuthToken;
import net.krotscheck.features.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.test.EnvironmentBuilder;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    private EnvironmentBuilder context;

    /**
     * The test context for a private application.
     */
    private EnvironmentBuilder authContext;

    /**
     * An oauth token.
     */
    private OAuthToken token;

    /**
     * A token with two scopes.
     */
    private OAuthToken scopedToken;

    /**
     * An expired token.
     */
    private OAuthToken expiredToken;

    /**
     * The auth header string for each test.
     */
    private String authHeader;

    /**
     * Bootstrap the application.
     */
    @Before
    public void bootstrap() {
        context = setupEnvironment()
                .role("debug")
                .client(ClientType.AuthorizationGrant)
                .authenticator("debug")
                .scope("debug")
                .scope("debug2")
                .user()
                .identity("test_identity_1")
                .bearerToken();

        // Create an auth token with a scope and refresh token.
        context.bearerToken();
        context.token(OAuthTokenType.Refresh,
                false, null, null, context.getToken());
        token = context.getToken();

        // Create a multiscope token.
        context.bearerToken();
        context.token(OAuthTokenType.Refresh,
                false, "debug debug2", null, context.getToken());
        scopedToken = context.getToken();

        // Create an expired token
        context.bearerToken();
        context.token(OAuthTokenType.Refresh, true, null, null,
                context.getToken());
        expiredToken = context.getToken();

        authContext = setupEnvironment()
                .role("debug")
                .client(ClientType.OwnerCredentials, true)
                .authenticator("debug")
                .scope("debug")
                .scope("debug2")
                .user()
                .identity("test_identity_2")
                .bearerToken()
                .refreshToken();
        authHeader = buildAuthorizationHeader(
                authContext.getClient().getId(),
                authContext.getClient().getClientSecret());
    }

    /**
     * Assert that a simple refresh request works.
     */
    @Test
    public void testTokenSimpleRequest() {
        Client c = context.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", token.getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) entity.getExpiresIn());
        assertNull(entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Assert that an authenticated refresh request works, via body
     * authentication.
     */
    @Test
    public void testAuthViaBodyRequest() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
        f.param("refresh_token", authContext.getToken().getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) entity.getExpiresIn());
        assertNull(entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Assert that an authenticated refresh request with bad credentials fails.
     */
    @Test
    public void testWrongAuthViaBodyRequest() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", "wrong_secret");
        f.param("refresh_token", authContext.getToken().getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_UNAUTHORIZED, r.getStatus());
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
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", authContext.getToken().getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) entity.getExpiresIn());
        assertNull(entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Assert that an authenticated refresh request with bad credentials fails.
     */
    @Test
    public void testWrongAuthViaHeaderRequest() {
        Client c = authContext.getClient();
        String badHeader = buildAuthorizationHeader(c.getId(), "badsecret");

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", authContext.getToken().getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", badHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_UNAUTHORIZED, r.getStatus());
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
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
        f.param("refresh_token", authContext.getToken().getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
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
        Client c = context.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", authContext.getToken().getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
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
        // Build the entity.
        Form f = new Form();
        f.param("refresh_token", token.getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
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
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", "invalid_token");
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_grant", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that an expired refresh token is not permitted.
     */
    @Test
    public void testExpiredRefreshToken() {
        Client c = context.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", expiredToken.getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
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
        Client c = context.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", token.getId().toString());
        f.param("grant_type", "refresh_token");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) entity.getExpiresIn());
        assertNull(entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());

        // Now do the whole thing again.
        Response r2 = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r2.getStatus());
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
        Client c = context.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", scopedToken.getId().toString());
        f.param("grant_type", "refresh_token");
        f.param("scope", "debug debug2");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) entity.getExpiresIn());
        assertEquals("debug debug2", entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Test that a refreshed token may request a subset of the authorization
     * scope.
     */
    @Test
    public void testScopeSubsetSelected() {
        Client c = context.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", scopedToken.getId().toString());
        f.param("grant_type", "refresh_token");
        f.param("scope", "debug2");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) entity.getExpiresIn());
        assertEquals("debug2", entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Test that a refreshed token may not escalate its scope in a refresh
     * request.
     */
    @Test
    public void testScopeEscalationFails() {
        Client c = context.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("refresh_token", token.getId().toString());
        f.param("grant_type", "refresh_token");
        f.param("scope", "debug debug2");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_scope", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }
}
