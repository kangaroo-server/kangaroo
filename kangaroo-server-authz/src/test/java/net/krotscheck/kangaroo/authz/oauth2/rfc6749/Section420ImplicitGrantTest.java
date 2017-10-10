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
import net.krotscheck.kangaroo.authz.common.database.entity.HttpSession;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.HttpUtil;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.math.BigInteger;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * These tests run through the Implicit Grant Flow.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2">https://tools.ietf.org/html/rfc6749#section-4.2</a>
 */
public final class Section420ImplicitGrantTest
        extends AbstractRFC6749Test {

    /**
     * The environment context for the regular client.
     */
    private static ApplicationContext context;

    /**
     * The environment context for the regular client and two redirects.
     */
    private static ApplicationContext twoRedirectContext;

    /**
     * An application with no role.
     */
    private static ApplicationContext noRoleContext;

    /**
     * An application with a role, but no scopes on that role.
     */
    private static ApplicationContext roleNoScopeContext;

    /**
     * The test context for a bare-bones application.
     */
    private static ApplicationContext bareContext;

    /**
     * An application without an authenticator.
     */
    private static ApplicationContext noauthContext;

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
                            .role("test", new String[]{"debug"})
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .build();
                    twoRedirectContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .redirect("http://other.example.com/redirect")
                            .build();
                    bareContext = ApplicationBuilder
                            .newApplication(session)
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Test)
                            .build();
                    noRoleContext = ApplicationBuilder
                            .newApplication(session)
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .build();
                    roleNoScopeContext = ApplicationBuilder
                            .newApplication(session)
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Test)
                            .scope("debug")
                            .redirect("http://valid.example.com/redirect")
                            .role("test", new String[]{})
                            .build();
                    noauthContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.Implicit)
                            .redirect("http://valid.example.com/redirect")
                            .build();
                }
            };

    /**
     * Test helper, asserts that the session is properly set.
     *
     * @param r The response to scan.
     */
    private HttpSession assertNewSession(final Response r) {
        Map<String, NewCookie> cookies = r.getCookies();
        assertTrue(cookies.containsKey("kangaroo"));
        NewCookie kangarooCookie = cookies.get("kangaroo");

        BigInteger sessionId = IdUtil.fromString(kangarooCookie.getValue());

        // Read the DB session
        Session hSession = getSession();
        hSession.beginTransaction();
        HttpSession dbSession = hSession.get(HttpSession.class, sessionId);
        hSession.getTransaction().commit();
        assertNotNull(dbSession);

        assertTrue(kangarooCookie.isHttpOnly());
        assertTrue(kangarooCookie.isSecure());
        // Can't be specific here, since there's a bit of latency in the test.
        assertTrue(kangarooCookie.getExpiry().compareTo(new Date()) > 0);
        assertEquals("localhost", kangarooCookie.getDomain());

        return dbSession;
    }

    /**
     * Assert that a session has been rotated between two requests.
     *
     * @param first  First response.
     * @param second Second response.
     */
    private void assertRotatedSession(final Response first,
                                      final Response second) {
        NewCookie firstCookie = first.getCookies().get("kangaroo");
        NewCookie secondCookie = second.getCookies().get("kangaroo");

        assertEquals(firstCookie.getMaxAge(), secondCookie.getMaxAge());
        // Can't test this reliably, as we might hit a time bridge.
        // assertEquals(firstCookie.getExpiry(), secondCookie.getExpiry());
        assertEquals(firstCookie.getDomain(), secondCookie.getDomain());
        assertEquals(firstCookie.getName(), secondCookie.getName());
        assertEquals(firstCookie.getPath(), secondCookie.getPath());

        assertNotEquals(firstCookie.getValue(), secondCookie.getValue());

        // Make sure the first cookie does not exist in the database, and the
        // second does.
        BigInteger firstCookieId = IdUtil.fromString(firstCookie.getValue());
        BigInteger secondCookieId = IdUtil.fromString(secondCookie.getValue());
        Session hSession = getSession();
        hSession.clear();
        hSession.beginTransaction();
        HttpSession firstDbSession = hSession.get(HttpSession.class,
                firstCookieId);
        HttpSession secondDbSession = hSession.get(HttpSession.class,
                secondCookieId);
        hSession.getTransaction().commit();

        assertNull(firstDbSession);
        assertNotNull(secondDbSession);
    }

    /**
     * Assert that a session has not been rotated between two requests.
     *
     * @param first  First response.
     */
    private void assertNoNewSession(final Response first) {
        assertNull(first.getCookies().get("kangaroo"));
    }

    /**
     * Assert that the session has a refresh token associated with it,
     * assigned to the provided OAuth2 token.
     *
     * @param params The query parameters to scan.
     */
    private void assertValidSessionRefreshToken(
            final MultivaluedMap<String, String> params) {
        String token = params.getFirst("access_token");
        BigInteger tokenId = IdUtil.fromString(token);
        OAuthToken bearerToken = getSession().get(OAuthToken.class, tokenId);
        assertNotNull(bearerToken);
        assertTrue(bearerToken.getTokenType().equals(OAuthTokenType.Bearer));

        // Get the refresh token.
        OAuthToken refreshToken =
                (OAuthToken) getSession()
                        .createCriteria(OAuthToken.class)
                        .add(Restrictions.eq("authToken", bearerToken))
                        .uniqueResult();
        assertNotNull(refreshToken);
        assertTrue(refreshToken.getTokenType().equals(OAuthTokenType.Refresh));
    }

    /**
     * Assert that a simple request works. This request requires the setup of a
     * default authenticator, a single redirect_uri, and a default scope.
     */
    @Test
    public void testAuthorizeSimpleRequest() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .request()
                .get();
        assertNewSession(first);

        // Follow the redirect
        Response second = followRedirect(first);
        assertRotatedSession(first, second);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
    }

    /**
     * Assert that a request with an invalid response type errors.
     */
    @Test
    public void testAuthorizeResponseTypeInvalid() {
        Response first = target("/authorize")
                .queryParam("response_type", "invalid")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .request()
                .get();
        assertNewSession(first);

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.FOUND.getStatusCode(), first.getStatus());

        // Validate the redirect location
        URI location = first.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());

        // Validate the query parameters received.
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertTrue(params.containsKey("error"));
        assertEquals("unsupported_response_type", params.getFirst("error"));
        assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with an invalid client id errors.
     */
    @Test
    public void testAuthorizeClientIdInvalid() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", "invalid_client_id")
                .request()
                .get();
        assertNoNewSession(first);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), first.getStatus());
        assertNull(first.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, first.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = first.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request with an explicit scope works.
     */
    @Test
    public void testAuthorizeScopeSimple() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .queryParam("scope", "debug")
                .request()
                .get();
        assertNewSession(first);

        // Follow the redirect
        Response second = followRedirect(first);
        assertNewSession(second);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertEquals("debug", params.getFirst("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a client with no authenticator rejects all requests.
     */
    @Test
    public void testAuthorizeNone() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(noauthContext.getClient().getId()))
                .queryParam("scope", "debug")
                .request()
                .get();
        assertNewSession(first);

        // Assert various response-specific parameters.
        assertEquals(Status.FOUND.getStatusCode(), first.getStatus());

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Validate the query parameters received.
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertTrue(params.containsKey("error"));
        assertEquals("invalid_request", params.getFirst("error"));
        assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with an invalid scope does not grant said scope.
     */
    @Test
    public void testAuthorizeScopeInvalid() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .queryParam("scope", "invalid")
                .request()
                .get();
        assertNewSession(first);

        // Follow the redirect
        Response second = followRedirect(first);
        assertRotatedSession(first, second);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with a state works.
     */
    @Test
    public void testAuthorizeStateSimple() {
        String state =
                IdUtil.toString(IdUtil.next());
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .queryParam("scope", "debug")
                .queryParam("state", state)
                .request()
                .get();
        assertNewSession(first);

        // Follow the redirect
        Response second = followRedirect(first);
        assertRotatedSession(first, second);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertEquals("debug", params.getFirst("scope"));
        assertEquals(state, params.getFirst("state"));
    }

    /**
     * Assert that a request with a redirect works.
     */
    @Test
    public void testAuthorizeRedirectSimple() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("scope", "debug")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .queryParam("redirect_uri", "http://valid.example.com/redirect")
                .request()
                .get();
        assertNewSession(first);

        // Follow the redirect
        Response second = followRedirect(first);
        assertRotatedSession(first, second);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertEquals("debug", params.getFirst("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with a different, registered redirect works.
     */
    @Test
    public void testAuthorizeRedirectMulti() {
        // Register a new redirect on the current builder.
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",

                        IdUtil.toString(twoRedirectContext.getClient().getId()))
                .queryParam("redirect_uri",
                        "http://other.example.com/redirect")
                .request()
                .get();
        assertNewSession(first);

        // Follow the redirect
        Response second = followRedirect(first);
        assertRotatedSession(first, second);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("other.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that no registered redirect fails when a default redirect is
     * requested.
     */
    @Test
    public void testAuthorizeRedirectNoneRegistered() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",

                        IdUtil.toString(bareContext.getClient().getId()))
                .request()
                .get();
        assertNewSession(first);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), first.getStatus());
        assertNull(first.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, first.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = first.readEntity(ErrorResponse.class);
        assertEquals("invalid_request", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that no registered redirect fails even when a redirect URI is
     * provided by the client.
     */
    @Test
    public void testAuthorizeRedirectNoneRegisteredWithRequest() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",

                        IdUtil.toString(bareContext.getClient().getId()))
                .queryParam("redirect_uri",
                        "http://redirect.example.com/redirect")
                .request()
                .get();
        assertNewSession(first);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), first.getStatus());
        assertNull(first.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, first.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = first.readEntity(ErrorResponse.class);
        assertEquals("invalid_request", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request to an application, which has more than one
     * registered redirect, with no redirect_uri parameter, will fail without
     * triggering a redirect.
     */
    @Test
    public void testAuthorizeRedirectMultiNoneProvided() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(twoRedirectContext.getClient().getId()))
                .request()
                .get();
        assertNewSession(first);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), first.getStatus());
        assertNull(first.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, first.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = first.readEntity(ErrorResponse.class);
        assertEquals("invalid_request", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request to an application, which has only one registered
     * redirect, will default to that registered redirect.
     */
    @Test
    public void testAuthorizeRedirectDefault() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",

                        IdUtil.toString(context.getClient().getId()))
                .request()
                .get();
        assertNewSession(first);

        // We expect this response to head to /authorize/redirect
        assertEquals(Status.FOUND.getStatusCode(), first.getStatus());
        URI firstLocation = first.getLocation();
        assertEquals("http", firstLocation.getScheme());
        assertEquals("localhost", firstLocation.getHost());
        assertEquals("/authorize/callback", firstLocation.getPath());

        // Follow the redirect
        Response second = followRedirect(first);
        assertRotatedSession(first, second);
        URI secondLocation = second.getLocation();

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(secondLocation.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with an invalid redirect fails.
     */
    @Test
    public void testAuthorizeRedirectInvalid() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",

                        IdUtil.toString(context.getClient().getId()))
                .queryParam("redirect_uri",
                        "http://invalid.example.com/redirect")
                .request()
                .get();
        assertNewSession(first);

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), first.getStatus());
        assertNull(first.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, first.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = first.readEntity(ErrorResponse.class);
        assertEquals("invalid_request", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request to an application, from a user which has no
     * assigned role, who is requesting scopes, will result in an invalid
     * scope response.
     */
    @Test
    public void testAuthorizeRedirectNoRole() {
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",

                        IdUtil.toString(noRoleContext.getClient().getId()))
                .request()
                .get();
        assertNewSession(first);

        // We expect this response to head to /authorize/redirect
        assertEquals(Status.FOUND.getStatusCode(), first.getStatus());
        URI firstLocation = first.getLocation();
        assertEquals("http", firstLocation.getScheme());
        assertEquals("localhost", firstLocation.getHost());
        assertEquals("/authorize/callback", firstLocation.getPath());

        // Follow the redirect
        Response second = followRedirect(first);
        assertNoNewSession(second);
        URI secondLocation = second.getLocation();

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(secondLocation.getFragment());
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
        // Fill out the bare context. The test authenticator will assign the
        // 'test' role if it finds it, so we create it here to ensure that it
        // has no permitted scopes.
        ApplicationContext testContext = bareContext.getBuilder()
                .scope("debug")
                .redirect("http://valid.example.com/redirect")
                .role("test", new String[]{})
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("scope", "debug")
                .queryParam("client_id",

                        IdUtil.toString(testContext.getClient().getId()))
                .request()
                .get();
        assertNewSession(first);

        // We expect this response to head to /authorize/redirect
        assertEquals(Status.FOUND.getStatusCode(), first.getStatus());
        URI firstLocation = first.getLocation();
        assertEquals("http", firstLocation.getScheme());
        assertEquals("localhost", firstLocation.getHost());
        assertEquals("/authorize/callback", firstLocation.getPath());

        // Follow the redirect
        Response second = followRedirect(first);
        assertNoNewSession(second);
        URI secondLocation = second.getLocation();

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(secondLocation.getFragment());
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
                .queryParam("response_type", "token")
                .queryParam("client_id",

                        IdUtil.toString(roleNoScopeContext.getClient().getId()))
                .request()
                .get();
        assertNewSession(first);

        // We expect this response to head to /authorize/redirect
        assertEquals(Status.FOUND.getStatusCode(), first.getStatus());
        URI firstLocation = first.getLocation();
        assertEquals("http", firstLocation.getScheme());
        assertEquals("localhost", firstLocation.getHost());
        assertEquals("/authorize/callback", firstLocation.getPath());

        // Follow the redirect
        Response second = followRedirect(first);
        assertRotatedSession(first, second);
        URI secondLocation = second.getLocation();

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(secondLocation.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a simple refresh request works. This presumes a refresh
     * token that is attached to an existing http session.
     */
    @Test
    public void testRefreshSimpleRequest() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .bearerToken()
                .refreshToken()
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();
        assertNewSession(first);
        assertNull(getSession().get(HttpSession.class,
                refreshContext.getHttpSession().getId()));

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
    }

    /**
     * Assert that a refresh request, using a refresh token whose access
     * token has already been cleaned up, can issue a new token.
     */
    @Test
    public void testRefreshSimpleRequestWithoutBearer() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .refreshToken()
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();
        assertNewSession(first);
        assertNull(getSession().get(HttpSession.class,
                refreshContext.getHttpSession().getId()));

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
    }

    /**
     * Assert that a request with an invalid response type errors.
     */
    @Test
    public void testRefreshResponseTypeInvalid() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .bearerToken()
                .refreshToken()
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "invalid")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();
        assertNotNull(getSession().get(HttpSession.class,
                refreshContext.getHttpSession().getId()));

        // Assert various response-specific parameters.
        Assert.assertEquals(Status.FOUND.getStatusCode(), first.getStatus());

        // Validate the redirect location
        URI location = first.getLocation();
        Assert.assertEquals("http", location.getScheme());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());

        // Validate the query parameters received.
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertTrue(params.containsKey("error"));
        assertEquals("unsupported_response_type", params.getFirst("error"));
        assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with an invalid client id errors.
     */
    @Test
    public void testRefreshClientIdInvalid() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .bearerToken()
                .refreshToken()
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", "invalid_client_id")
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();

        // Assert various response-specific parameters.
        assertEquals(Status.BAD_REQUEST.getStatusCode(), first.getStatus());
        assertNull(first.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, first.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = first.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request with an explicit scope works.
     */
    @Test
    public void testRefreshScopeSimple() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .bearerToken("debug")
                .refreshToken()
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .queryParam("scope", "debug")
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();
        assertNewSession(first);

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertEquals("debug", params.getFirst("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with an invalid scope does not grant said scope.
     */
    @Test
    public void testRefreshScopeInvalid() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .bearerToken("debug")
                .refreshToken()
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .queryParam("scope", "invalid")
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with a state works.
     */
    @Test
    public void testRefreshStateSimple() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .bearerToken("debug")
                .refreshToken()
                .httpSession(false)
                .build();

        String state = IdUtil.toString(IdUtil.next());
        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .queryParam("scope", "debug")
                .queryParam("state", state)
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();
        assertNewSession(first);

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertEquals("debug", params.getFirst("scope"));
        assertEquals(state, params.getFirst("state"));
    }

    /**
     * Assert that a request with a redirect works.
     */
    @Test
    public void testRefreshRedirectSimple() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .token(OAuthTokenType.Bearer, false, "debug",
                        "http://valid.example.com/redirect", null)
                .refreshToken()
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("scope", "debug")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .queryParam("redirect_uri", "http://valid.example.com/redirect")
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();
        assertNewSession(first);

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertEquals("debug", params.getFirst("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a refresh request, with a session that is holding a
     * refresh token for a different client, kicks off a normal auth flow.
     */
    @Test
    public void testRefreshClientIsolation() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .bearerToken()
                .refreshToken()
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(roleNoScopeContext.getClient().getId()))
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();
        assertNewSession(first);

        // Follow the redirect
        Response second = followRedirect(first);
        assertRotatedSession(first, second);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertValidBearerToken(params, true);
        assertValidSessionRefreshToken(params);
        assertFalse(params.containsKey("scope"));
    }

    /**
     * Assert that a refresh request, with a session that is holding an
     * expired token, kicks off a normal auth flow.
     */
    @Test
    public void testRefreshWithExpiredToken() {
        ApplicationContext refreshContext = context.getBuilder()
                .user()
                .identity()
                .bearerToken("debug")
                .build();
        OAuthToken original = refreshContext.getToken();
        refreshContext = refreshContext.getBuilder()
                .token(OAuthTokenType.Refresh, true, null, null, original)
                .httpSession(false)
                .build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .queryParam("scope", "debug")
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("localhost", location.getHost());
        assertEquals("/authorize/callback", location.getPath());
    }

    /**
     * Assert that a refresh request, with a session which for some reason
     * has two valid refresh tokens attached, deletes all tokens and kicks
     * off a new auth flow.
     */
    @Test
    public void testRefreshWithTooManyTokens() {
        ApplicationBuilder refreshBuilder = context.getBuilder()
                .user()
                .identity()
                .bearerToken("debug")
                .refreshToken()
                .httpSession(false)
                .refreshToken();

        // Make modifications to the latter refreshToken.
        refreshBuilder.getContext().getToken().setHttpSession(
                refreshBuilder.getContext().getHttpSession());
        ApplicationContext refreshContext = refreshBuilder.build();

        Response first = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        IdUtil.toString(refreshContext.getClient().getId()))
                .queryParam("scope", "debug")
                .request()
                .cookie("kangaroo", refreshContext.getHttpSessionId())
                .get();

        // Validate the redirect location
        URI location = first.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("localhost", location.getHost());
        assertEquals("/authorize/callback", location.getPath());

        // Make sure the refresh tokens are gone.
        Session s = getSession();
        s.beginTransaction();
        assertNull(s.get(OAuthToken.class, refreshContext.getToken().getId()));
        s.getTransaction().commit();
    }
}
