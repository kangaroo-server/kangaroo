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
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.apache.http.HttpHeaders;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.math.BigInteger;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    private static ApplicationContext context;
    /**
     * The test context for a bare application.
     */
    private static ApplicationContext bareContext;
    /**
     * The test context for an application with a user that has no roles.
     */
    private static ApplicationContext noUserRoleContext;
    /**
     * A scope with a role, but not scope assigned.
     */
    private static ApplicationContext noScopeRoleContext;
    /**
     * The test context for an authenticated application.
     */
    private static ApplicationContext authContext;
    /**
     * The test context for an application with no authenticator.
     */
    private static ApplicationContext noauthContext;
    /**
     * The test context for an application with an authenticator that has no
     * implementation.
     */
    private static ApplicationContext misconfiguredAuthContext;
    /**
     * An invalid client context.
     */
    private static ApplicationContext invalidClientContext;
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
                    context = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .scope("debug1")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.AuthorizationGrant)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .user()
                            .identity()
                            .build();
                    bareContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .client(ClientType.AuthorizationGrant)
                            .authenticator(AuthenticatorType.Test)
                            .user()
                            .identity()
                            .build();
                    noScopeRoleContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .role("test", new String[]{})
                            .client(ClientType.AuthorizationGrant)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .build();
                    noUserRoleContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .client(ClientType.AuthorizationGrant)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .build();
                    authContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.AuthorizationGrant, true)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .redirect("http://redirect.example.com/redirect")
                            .user()
                            .identity("remote_identity")
                            .build();
                    noauthContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.AuthorizationGrant)
                            .redirect("http://valid.example.com/redirect")
                            .build();
                    misconfiguredAuthContext = ApplicationBuilder
                            .newApplication(session)
                            .client(ClientType.AuthorizationGrant)
                            .authenticator(AuthenticatorType.Password)
                            .redirect("http://valid.example.com/redirect")
                            .scope("debug")
                            .build();
                    invalidClientContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Password)
                            .redirect("http://valid.example.com/redirect")
                            .build();

                    authHeader = HttpUtil.authHeaderBasic(
                            authContext.getClient().getId(),
                            authContext.getClient().getClientSecret());
                }
            };

    /**
     * Assert that a simple request works. This request requires the setup of a
     * default authenticator, a single redirect_uri, and a default scope.
     * <p>
     * Preconditions:
     * - valid_client_id
     * - one single authenticator (debug)
     * - one single redirect url (http://valid.example.com/redirect)
     */
    @Test
    public void testAuthorizeSimpleRequest() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", IdUtil.toString(context.getClient().getId()))
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Test that, if the client provides an authorization password, that
     * authentication using that password via the Authorization header works.
     */
    @Test
    public void testAuthorizeAuthHeaderValid() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", "http://valid.example.com/redirect")
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
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
                .queryParam("client_id", IdUtil.toString(IdUtil.next()))
                .request()
                .header("Authorization", authHeader)
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("access_denied", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Test that a user may identify themselves solely via the
     * Authorization header.
     */
    @Test
    public void testAuthorizeAuthHeaderValidNoExplicitClientId() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", "http://valid.example.com/redirect")
                .request()
                .header("Authorization", authHeader)
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.FOUND.getStatusCode(), r.getStatus());
        Assert.assertNotNull(r.getLocation());
    }

    /**
     * Test that authorization via the header with a bad password fails.
     */
    @Test
    public void testAuthorizeAuthHeaderInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(authContext.getClient().getId()))
                .request()
                .header("Authorization", "badpassword")
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("access_denied", error.getError());
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
                        IdUtil.toString(authContext.getClient().getId()))
                .queryParam("client_secret",
                        authContext.getClient().getClientSecret())
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("bad_request", error.getError());
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
                        IdUtil.toString(context.getClient().getId()))
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.FOUND.getStatusCode(), r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("error"));
        Assert.assertEquals("unsupported_response_type",
                params.getFirst("error"));
        Assert.assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with an malformed client id errors.
     */
    @Test
    public void testAuthorizeClientIdMalformed() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", "malformed_client_id")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("bad_request", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request with an invalid client id errors.
     */
    @Test
    public void testAuthorizeClientIdInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(IdUtil.next()))
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("access_denied", error.getError());
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
                        IdUtil.toString(context.getClient().getId()))
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
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
                        IdUtil.toString(noauthContext.getClient().getId()))
                .queryParam("scope", "debug")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.FOUND.getStatusCode(), r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("error"));
        Assert.assertEquals("invalid_request", params.getFirst("error"));
        Assert.assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a client with an unimplemented authenticator fails.
     */
    @Test
    public void testAuthorizeNotConfiguredAuthenticator() {
        Response r = target("/authorize")
                .queryParam("authenticator", "Test")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(misconfiguredAuthContext.getClient()
                                .getId()))
                .queryParam("scope", "debug")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.FOUND.getStatusCode(), r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("error"));
        Assert.assertEquals("invalid_request", params.getFirst("error"));
        Assert.assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with an invalid scope does not grant said scope.
     */
    @Test
    public void testAuthorizeScopeInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .queryParam("scope", "invalid")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.FOUND.getStatusCode(), second.getStatus());

        // Validate the redirect location
        URI location = second.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertNull(location.getFragment());

        // Validate the query parameters received.
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
        Assert.assertFalse(params.containsKey("scope"));
    }

    /**
     * Assert that a request with a state works.
     */
    @Test
    public void testAuthorizeStateSimple() {
        String state = IdUtil.toString(IdUtil.next());
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertEquals(state, params.getFirst("state"));
    }

    /**
     * Assert that a request with a redirect works.
     */
    @Test
    public void testAuthorizeRedirectSimple() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with a redirect and a state works.
     */
    @Test
    public void testAuthorizeRedirectState() {
        BigInteger state = IdUtil.next();
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("state", state.toString())
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertEquals(state.toString(), params.getFirst("state"));
    }

    /**
     * Assert that a request with a different, registered redirect works.
     */
    @Test
    public void testAuthorizeRedirectMulti() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri",
                        "http://redirect.example.com/redirect")
                .request()
                .header("Authorization", authHeader)
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
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
                        IdUtil.toString(bareContext.getClient().getId()))
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
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
                        IdUtil.toString(bareContext.getClient().getId()))
                .queryParam("redirect_uri",
                        "http://redirect.example.com/redirect")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
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
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .request()
                .header("Authorization", authHeader)
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
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
                        IdUtil.toString(context.getClient().getId()))
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
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
                        IdUtil.toString(context.getClient().getId()))
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
        Assert.assertTrue(params.containsKey("foo"));
        Assert.assertEquals("bar", params.getFirst("foo"));
    }

    /**
     * Assert that a request with an invalid redirect fails.
     */
    @Test
    public void testAuthorizeRedirectInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .queryParam("redirect_uri",
                        "http://invalid.example.com/redirect")
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertNull(r.getLocation());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request to an application, from a user which has no
     * assigned role, who is requesting scopes, will result in an invalid
     * scope response.
     */
    @Test
    public void testAuthorizeRedirectNoRole() {
        Response first = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(noUserRoleContext.getClient().getId()))
                .request()
                .get();

        // We expect this response to head to /authorize/redirect
        assertEquals(Status.FOUND.getStatusCode(), first.getStatus());
        URI firstLocation = first.getLocation();
        assertEquals("http", firstLocation.getScheme());
        assertEquals("localhost", firstLocation.getHost());
        assertEquals("/authorize/callback", firstLocation.getPath());

        // Follow the redirect
        Response second = followRedirect(first);
        URI secondLocation = second.getLocation();

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(secondLocation);
        assertTrue(params.containsKey("error"));
        assertEquals("invalid_scope", params.getFirst("error"));
        assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request to an application, from a user which has a role,
     * but who may not access the requested scope, will result in an invalid
     * scope response.
     */
    @Test
    public void testAuthorizeRedirectRoleWithoutRequestedScope() {
        Response first = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("scope", "debug1")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .request()
                .get();

        // We expect this response to head to /authorize/redirect
        assertEquals(Status.FOUND.getStatusCode(), first.getStatus());
        URI firstLocation = first.getLocation();
        assertEquals("http", firstLocation.getScheme());
        assertEquals("localhost", firstLocation.getHost());
        assertEquals("/authorize/callback", firstLocation.getPath());

        // Follow the redirect
        Response second = followRedirect(first);
        URI secondLocation = second.getLocation();

        // Extract the query parameters in the fragment

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(secondLocation);
        assertTrue(params.containsKey("error"));
        assertEquals("invalid_scope", params.getFirst("error"));
        assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request to an application, from a user which has a role,
     * but no scopes, may still request a token with no assigned scope.
     */
    @Test
    public void testAuthorizeRedirectRoleWantsNoScope() {
        Response first = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(noScopeRoleContext.getClient().getId()))
                .request()
                .get();

        // We expect this response to head to /authorize/redirect
        assertEquals(Status.FOUND.getStatusCode(), first.getStatus());
        URI firstLocation = first.getLocation();
        assertEquals("http", firstLocation.getScheme());
        assertEquals("localhost", firstLocation.getHost());
        assertEquals("/authorize/callback", firstLocation.getPath());

        // Follow the redirect
        Response second = followRedirect(first);
        URI secondLocation = second.getLocation();

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(secondLocation);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a simple token request works.
     */
    @Test
    public void testTokenSimpleRequest() {
        OAuthToken token = context.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
    }

    /**
     * Assert that missing a client id errors.
     */
    @Test
    public void testTokenNoClientId() {
        OAuthToken token = context.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("access_denied", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that missing an auth code errors.
     */
    @Test
    public void testTokenNoCode() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that missing a token type errors.
     */
    @Test
    public void testTokenNoGrant() {
        OAuthToken token = context.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that missing an redirect errors.
     */
    @Test
    public void testTokenNoRedirect() {
        OAuthToken token = context.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that, if the client provides a token password, that
     * authentication using that password via the Authorization header works.
     */
    @Test
    public void testTokenAuthHeaderValid() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://redirect.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
        Assert.assertNull(entity.getScope());
    }

    /**
     * Test that a user that provides a mismatched client_id in the request
     * body and the Authorization header fails.
     */
    @Test
    public void testTokenAuthHeaderMismatchClientId() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", "other_client_id");
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("bad_request", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that a request may identify themselves solely via the
     * authorization header.
     */
    @Test
    public void testTokenAuthHeaderValidNoExplicitClientId() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://redirect.example.com/redirect");
        f.param("code", IdUtil.toString(token.getId()));
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
    }

    /**
     * Test that authorization via the header with a bad password fails.
     */
    @Test
    public void testTokenAuthHeaderInvalid() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        String badHeader = HttpUtil.authHeaderBasic(
                authContext.getClient().getId(),
                "invalid_secret");

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(authContext.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header(HttpHeaders.AUTHORIZATION, badHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("access_denied", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that a client may also authenticate by putting the client_secret
     * in the post body.
     */
    @Test
    public void testTokenAuthSecretInBody() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(authContext.getClient().getId()));
        f.param("client_secret", authContext.getClient().getClientSecret());
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://redirect.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
    }

    /**
     * Assert that two identification methods may be used, IFF they're for
     * the same client.
     */
    @Test
    public void testTokenAuthBothMethods() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(authContext.getClient().getId()));
        f.param("client_secret", authContext.getClient().getClientSecret());
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://redirect.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
    }

    /**
     * Assert that an invalid token type errors.
     */
    @Test
    public void testTokenInvalidGrantTypePassword() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "password");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an invalid token type errors.
     */
    @Test
    public void testTokenInvalidGrantTypeRefreshToken() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "refresh_token");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an invalid token type errors.
     */
    @Test
    public void testTokenInvalidGrantTypeClientCredentials() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "client_credentials");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an unknown token type errors.
     */
    @Test
    public void testTokenUnknownGrantType() {
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "unknown_grant_type");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that a request from an invalid client fails.
     */
    @Test
    public void testTokenInvalidClient() {
        // Create the test token.
        ApplicationContext testContext = invalidClientContext.getBuilder()
                .authToken()
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(testContext.getClient().getId()));
        f.param("code", IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an invalid authorization code errors.
     */
    @Test
    public void testTokenInvalidCode() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(IdUtil.next()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an invalid authorization code errors.
     */
    @Test
    public void testTokenMalformedCode() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", "not_a_BigInteger");
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("bad_request", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an expired authorization code errors.
     */
    @Test
    public void testTokenExpiredCode() {
        // Add an expired token.
        OAuthToken token = context.getBuilder()
                .token(OAuthTokenType.Authorization, true, null, null, null)
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an valid code, that mismatches the client id, fails.
     */
    @Test
    public void testTokenCodeClientMismatch() {
        // Create the test token.
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an valid code, that mismatches the client's redirect, fails.
     */
    @Test
    public void testTokenCodeRedirectMismatch() {
        // Create the test token.
        OAuthToken token = context.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://other.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an valid code, that mismatches the token's redirect, even
     * if it is a valid redirect location for the client, fails.
     */
    @Test
    public void testTokenMultiCodeRedirectMismatch() {
        OAuthToken token = context.getBuilder()
                .token(OAuthTokenType.Authorization,
                        false, null, "http://valid.example.com/redirect", null)
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://other.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that a request with a redirect works.
     */
    @Test
    public void testTokenRedirectSimple() {
        OAuthToken token = context.getBuilder()
                .token(OAuthTokenType.Authorization, false, null,
                        "http://valid.example.com/redirect", null)
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
        Assert.assertNull(entity.getScope());
    }

    /**
     * Assert that a request with a different, registered redirect works.
     */
    @Test
    public void testTokenRedirectMulti() {
        OAuthToken token = authContext.getBuilder()
                .token(OAuthTokenType.Authorization,
                        false, null, "http://other.example.com/redirect", null)
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://other.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
        Assert.assertNull(entity.getScope());
    }

    /**
     * Assert that a request to an application, which has more than one
     * registered redirect, with no redirect_uri parameter, fails.
     */
    @Test
    public void testTokenRedirectMultiNoneProvided() {
        // Create the test token.
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that a request to an application, which has only one registered
     * redirect, will fail if no redirect has been provided.
     */
    @Test
    public void testTokenRedirectDefault() {
        // Create the test token.
        OAuthToken token = authContext.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that a request with a redirect_uri that includes an additional
     * query string is honored, as long as the base uri is registered, and
     * the previous authorization request received the same redirect.
     */
    @Test
    public void testTokenRedirectPartial() {
        ApplicationContext testContext = context.getBuilder()
                .token(OAuthTokenType.Authorization, false, null,
                        "http://valid.example.com/redirect?foo=bar", null)
                .build();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(testContext.getClient().getId()));
        f.param("code",
                IdUtil.toString(testContext.getToken().getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect?foo=bar");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
        Assert.assertNull(entity.getScope());
    }

    /**
     * Assert that a request with a redirect_uri that includes an additional
     * query string, but whose additional query string does not match the
     * query string of the original authorization request, fails.
     */
    @Test
    public void testTokenRedirectPartialMismatch() {
        OAuthToken token = context.getBuilder()
                .token(OAuthTokenType.Authorization, false, null,
                        "http://valid.example.com/redirect?foo=bar", null)
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id",
                IdUtil.toString(context.getClient().getId()));
        f.param("code",
                IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect?lol=cat");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that a request with an invalid redirect fails.
     */
    @Test
    public void testTokenRedirectInvalid() {
        // Create the test token.
        OAuthToken token = context.getBuilder()
                .authToken()
                .build()
                .getToken();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", IdUtil.toString(token.getId()));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://other.example.com/redirect");
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_grant", entity.getError());
        Assert.assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test the full authorization flow.
     */
    @Test
    public void testFullAuthorizationFlow() {
        String state1 = IdUtil.toString(IdUtil.next());
        Response r = target("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .queryParam("scope", "debug")
                .queryParam("state", state1)
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
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location);
        Assert.assertTrue(params.containsKey("code"));
        Assert.assertEquals(state1, params.getFirst("state"));

        // Extract the authorization code and issue a token request
        String state2 = IdUtil.toString(IdUtil.next());
        Form f = new Form();
        f.param("client_id", IdUtil.toString(context.getClient().getId()));
        f.param("code", params.getFirst("code"));
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", "http://valid.example.com/redirect");
        f.param("state", state2);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response tr = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.OK.getStatusCode(), tr.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, tr.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = tr.readEntity(TokenResponseEntity.class);
        assertValidBearerToken(entity, true);
        Assert.assertEquals("debug", entity.getScope());
        Assert.assertEquals(state2, entity.getState());
    }
}
