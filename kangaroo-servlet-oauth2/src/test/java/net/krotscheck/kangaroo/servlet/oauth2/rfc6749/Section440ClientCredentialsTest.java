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

package net.krotscheck.kangaroo.servlet.oauth2.rfc6749;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientConfig;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.servlet.oauth2.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.HttpUtil;
import net.krotscheck.kangaroo.test.IFixture;
import org.apache.http.HttpStatus;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * These tests run through the Client Credentials Flow. In this context, the
 * client authenticates only via their clientID and client secret.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.4">https://tools.ietf.org/html/rfc6749#section-4.4</a>
 */
public final class Section440ClientCredentialsTest
        extends AbstractRFC6749Test {

    /**
     * Generic context.
     */
    private EnvironmentBuilder context;

    /**
     * The test context for an authenticated application.
     */
    private EnvironmentBuilder authContext;

    /**
     * The auth header string for each test.
     */
    private String authHeader;

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        context = new EnvironmentBuilder(getSession())
                .scope("debug")
                .client(ClientType.ClientCredentials, false);
        authContext = new EnvironmentBuilder(getSession())
                .scope("debug")
                .client(ClientType.ClientCredentials, true);
        authHeader = HttpUtil.authHeaderBasic(
                authContext.getClient().getId(),
                authContext.getClient().getClientSecret());

        List<IFixture> fixtures = new ArrayList<>();
        fixtures.add(context);
        fixtures.add(authContext);
        return fixtures;
    }

    /**
     * Load the test data.
     *
     * @return The test data.
     */
    @Override
    public File testData() {
        return null;
    }

    /**
     * Assert that a simple token request works.
     */
    @Test
    public void testTokenSimpleRequest() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
        f.param("grant_type", "client_credentials");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNull(entity.getRefreshToken());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) entity.getExpiresIn());
        assertNull(entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Assert that a bad password errors.
     */
    @Test
    public void testBadPassword() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", "invalid_secret");
        f.param("grant_type", "client_credentials");

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
     * Assert that missing a client id errors.
     */
    @Test
    public void testTokenNoClientId() {
        // Build the entity.
        Form f = new Form();
        f.param("grant_type", "client_credentials");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        // Make the request
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that missing a grant type errors.
     */
    @Test
    public void testTokenNoGrant() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
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
     * Assert that we can authenticate via the auth header.
     */
    @Test
    public void testTokenAuthHeaderValid() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("grant_type", "client_credentials");
        f.param("scope", "debug");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNull(entity.getRefreshToken());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) entity.getExpiresIn());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
        assertEquals("debug", entity.getScope());
    }

    /**
     * Test that a user that provides a mismatched client_id in the request
     * body and the Authorization header fails.
     */
    @Test
    public void testTokenAuthHeaderMismatchClientId() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", context.getClient().getId().toString());
        f.param("grant_type", "client_credentials");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
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
     * Test that a user may not identify themselves solely via the
     * Authorization header.
     */
    @Test
    public void testTokenAuthHeaderValidNoExplicitClientId() {
        // Build the entity.
        Form f = new Form();
        f.param("grant_type", "client_credentials");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
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
     * Test that authorization via the header with a bad client_credentials
     * fails.
     */
    @Test
    public void testTokenAuthHeaderInvalid() {
        Client c = authContext.getClient();
        String authHeader = HttpUtil.authHeaderBasic(c.getId(), "badsecret");

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("grant_type", "client_credentials");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
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
     * Assert that only one authentication method may be used.
     */
    @Test
    public void testTokenAuthBothMethods() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
        f.param("grant_type", "client_credentials");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
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
     * Assert that an invalid grant type errors.
     */
    @Test
    public void testTokenInvalidGrantTypePassword() {
        Client c = authContext.getClient();
        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
        f.param("grant_type", "password");
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
     * Assert that an invalid grant type errors.
     */
    @Test
    public void testTokenInvalidGrantTypeRefreshToken() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
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
     * Assert that an unknown grant type errors.
     */
    @Test
    public void testTokenUnknownGrantType() {
        Client c = authContext.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
        f.param("grant_type", "unknown_grant_type");
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
}
