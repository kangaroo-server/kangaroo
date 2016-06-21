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

import net.krotscheck.features.database.entity.Client;
import net.krotscheck.features.database.entity.ClientType;
import net.krotscheck.features.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.test.EnvironmentBuilder;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * These tests run through the Authorization Code Grant Flow.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4">https://tools.ietf.org/html/rfc6749#section-4</a>
 */
public final class Section410AuthorizationCodeGrantTest
        extends AbstractRFC6749Test {

    /**
     * The test context for a regular application.
     */
    private EnvironmentBuilder context;

    /**
     * The test context for a bare application.
     */
    private EnvironmentBuilder bareContext;

    /**
     * The test context for an authenticated application.
     */
    private EnvironmentBuilder authContext;

    /**
     * The test context for an application with no authenticator.
     */
    private EnvironmentBuilder noauthContext;

    /**
     * The test context for an application with an authenticator that has no
     * implementation.
     */
    private EnvironmentBuilder misconfiguredAuthContext;

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
                .authenticator("test")
                .redirect("http://valid.example.com/redirect")
                .scope("debug")
                .authToken();
        bareContext = setupEnvironment()
                .role("debug")
                .client(ClientType.AuthorizationGrant)
                .authenticator("test");
        authContext = setupEnvironment()
                .role("debug")
                .client(ClientType.AuthorizationGrant, true)
                .authenticator("test")
                .redirect("http://valid.example.com/redirect")
                .user()
                .identity("remote_identity")
                .authToken();
        noauthContext = setupEnvironment()
                .role("debug")
                .client(ClientType.AuthorizationGrant)
                .redirect("http://valid.example.com/redirect")
                .scope("debug");
        misconfiguredAuthContext = setupEnvironment()
                .client(ClientType.AuthorizationGrant)
                .authenticator("foo")
                .redirect("http://valid.example.com/redirect")
                .scope("debug");

        authHeader = buildAuthorizationHeader(
                authContext.getClient().getId(),
                authContext.getClient().getClientSecret());
    }

    /**
     * Assert that a simple request works. This request requires the setup of a
     * default authenticator, a single redirect_uri, and a default scope.
     *
     * Preconditions:
     * - valid_client_id
     * - one single authenticator (debug)
     * - one single redirect url (http://valid.example.com/redirect)
     */
    @Test
    public void testAuthorizeSimpleRequest() {
        Client c = context.getClient();

        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", c.getId().toString())
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Test that, if the client provides an authorization password, that
     * authentication using that password via the Authorization header works.
     */
    @Test
    public void testAuthorizeAuthHeaderValid() {
        Client c = authContext.getClient();
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", c.getId().toString())
                .request()
                .header("Authorization", authHeader)
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Test that a user that provides a mismatched client_id in the request
     * body and the Authorization header fails.
     */
    @Test
    public void testAuthorizeAuthHeaderMismatchClientId() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", UUID.randomUUID().toString())
                .request()
                .header("Authorization", authHeader)
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_client", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Test that a user may not identify themselves solely via the
     * Authorization
     * header.
     */
    @Test
    public void testAuthorizeAuthHeaderValidNoExplicitClientId() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .request()
                .header("Authorization", authHeader)
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_client", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Test that authorization via the header with a bad password fails.
     */
    @Test
    public void testAuthorizeAuthHeaderInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        authContext.getClient().getId().toString())
                .request()
                .header("Authorization", "badpassword")
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_client", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Test that putting a valid password into the client_secret url fails.
     */
    @Test
    public void testAuthorizeAuthSecretInUrl() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        authContext.getClient().getId().toString())
                .queryParam("client_secret",
                        authContext.getClient().getClientSecret())
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_client", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request with an invalid response type errors.
     */
    @Test
    public void testAuthorizeResponseTypeInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "invalid")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("error"));
        Assert.assertEquals("unsupported_response_type",
                params.get("error"));
        Assert.assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with an invalid client id errors.
     */
    @Test
    public void testAuthorizeClientIdInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", "invalid_client_id")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_client", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request with an explicit scope works.
     */
    @Test
    public void testAuthorizeScopeSimple() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("scope", "debug")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a client with no authenticator rejects all scopes.
     */
    @Test
    public void testAuthorizeNoAuthenticator() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        noauthContext.getClient().getId().toString())
                .queryParam("scope", "debug")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("error"));
        Assert.assertEquals("invalid_request", params.get("error"));
        Assert.assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a client with an unimplemented authenticator fails.
     */
    @Test
    public void testAuthorizeUnimplementedAuthenticator() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        misconfiguredAuthContext.getClient().getId().toString())
                .queryParam("scope", "debug")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("error"));
        Assert.assertEquals("invalid_request", params.get("error"));
        Assert.assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with an invalid scope errors.
     */
    @Test
    public void testAuthorizeScopeInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("scope", "invalid")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("error"));
        Assert.assertEquals("invalid_scope", params.get("error"));
        Assert.assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with a state works.
     */
    @Test
    public void testAuthorizeStateSimple() {
        String state = UUID.randomUUID().toString();
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("scope", "debug")
                .queryParam("state", state)
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertEquals(state, params.get("state"));
    }

    /**
     * Assert that a request with a redirect works.
     */
    @Test
    public void testAuthorizeRedirectSimple() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("redirect_uri", "http://valid.example.com/redirect")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with a redirect and a state works.
     */
    @Test
    public void testAuthorizeRedirectState() {
        UUID state = UUID.randomUUID();
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("state", state.toString())
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("redirect_uri", "http://valid.example.com/redirect")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertEquals(state.toString(), params.get("state"));
    }

    /**
     * Assert that a request with a different, registered redirect works.
     */
    @Test
    public void testAuthorizeRedirectMulti() {
        // Add another redirect.
        context.redirect("http://redirect.example.com/redirect");

        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("redirect_uri",
                        "http://redirect.example.com/redirect")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("redirect.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the body parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that no registered redirect fails when a default redirect is
     * requested.
     */
    @Test
    public void testAuthorizeRedirectNoneRegistered() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        bareContext.getClient().getId().toString())
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that no registered redirect fails even when a redirect URI is
     * provided by the client.
     */
    @Test
    public void testAuthorizeRedirectNoneRegisteredWithRequest() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        bareContext.getClient().getId().toString())
                .queryParam("redirect_uri",
                        "http://redirect.example.com/redirect")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request to an application, which has more than one
     * registered redirect, with no redirect_uri parameter, will fail without
     * triggering a redirect.
     */
    @Test
    public void testAuthorizeRedirectMultiNoneProvided() {
        context.redirect("http://redirect.example.com/redirect");

        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request to an application, which has only one registered
     * redirect, will default to that registered redirect.
     */
    @Test
    public void testAuthorizeRedirectDefault() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with a redirect_uri that includes an additional
     * query string is honored, as long as all registered components (query
     * parameters, path, host, etc) are present.
     */
    @Test
    public void testAuthorizeRedirectPartial() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("redirect_uri",
                        "http://valid.example.com/redirect?foo=bar")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        Map<String, String> params = parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
        Assert.assertTrue(params.containsKey("foo"));
        Assert.assertEquals("bar", params.get("foo"));
    }

    /**
     * Assert that a request with an invalid redirect fails.
     */
    @Test
    public void testAuthorizeRedirectInvalid() {
        context.redirect("http://redirect.example.com/redirect");

        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("redirect_uri",
                        "http://invalid.example.com/redirect")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }
}
