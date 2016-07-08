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

import net.krotscheck.kangaroo.database.entity.ClientConfig;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.HttpUtil;
import net.krotscheck.kangaroo.test.IFixture;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    private EnvironmentBuilder context;

    /**
     * The test context for a bare-bones application.
     */
    private EnvironmentBuilder bareContext;

    /**
     * An application without an authenticator.
     */
    private EnvironmentBuilder noauthContext;

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        context = new EnvironmentBuilder(getSession())
                .client(ClientType.Implicit)
                .authenticator("test")
                .scope("debug")
                .redirect("http://valid.example.com/redirect");
        bareContext = new EnvironmentBuilder(getSession())
                .client(ClientType.Implicit)
                .authenticator("test");
        noauthContext = new EnvironmentBuilder(getSession())
                .client(ClientType.Implicit)
                .scope("debug")
                .redirect("http://valid.example.com/redirect");

        List<IFixture> fixtures = new ArrayList<>();
        fixtures.add(context);
        fixtures.add(bareContext);
        fixtures.add(noauthContext);
        return fixtures;
    }

    /**
     * Assert that a simple request works. This request requires the setup of a
     * default authenticator, a single redirect_uri, and a default scope.
     */
    @Test
    public void testAuthorizeSimpleRequest() {
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", context.getClient().getId().toString())
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location
                        .getFragment());
        assertTrue(params.containsKey("access_token"));
        assertEquals("Bearer", params.getFirst("token_type"));
        assertEquals(ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                Integer.valueOf(params.getFirst("expires_in")));
        assertFalse(params.containsKey("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with an invalid response type errors.
     */
    @Test
    public void testAuthorizeResponseTypeInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "invalid")
                .queryParam("client_id", context.getClient().getId().toString())
                .request()
                .get();

        // Assert various response-specific parameters.
        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
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
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", "invalid_client_id")
                .request()
                .get();

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertNull(r.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that a request with an explicit scope works.
     */
    @Test
    public void testAuthorizeScopeSimple() {
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", context.getClient().getId().toString())
                .queryParam("scope", "debug")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertTrue(params.containsKey("access_token"));
        assertEquals("Bearer", params.getFirst("token_type"));
        assertEquals(ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                Integer.valueOf(params.getFirst("expires_in")));
        assertEquals("debug", params.getFirst("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a client with no authenticator rejects all requests.
     */
    @Test
    public void testAuthorizeNone() {
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", noauthContext.getClient().getId())
                .queryParam("scope", "debug")
                .request()
                .get();

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
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
     * Assert that a request with an invalid scope errors.
     */
    @Test
    public void testAuthorizeScopeInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", context.getClient().getId().toString())
                .queryParam("scope", "invalid")
                .request()
                .get();

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());

        // Validate the redirect location
        URI location = r.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Validate the query parameters received.
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertTrue(params.containsKey("error"));
        assertEquals("invalid_scope", params.getFirst("error"));
        assertTrue(params.containsKey("error_description"));
    }

    /**
     * Assert that a request with a state works.
     */
    @Test
    public void testAuthorizeStateSimple() {
        String state = UUID.randomUUID().toString();
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", context.getClient().getId().toString())
                .queryParam("scope", "debug")
                .queryParam("state", state)
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertTrue(params.containsKey("access_token"));
        assertEquals("Bearer", params.getFirst("token_type"));
        assertEquals(ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                Integer.valueOf(params.getFirst("expires_in")));
        assertEquals("debug", params.getFirst("scope"));
        assertEquals(state, params.getFirst("state"));
    }

    /**
     * Assert that a request with a redirect works.
     */
    @Test
    public void testAuthorizeRedirectSimple() {
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("scope", "debug")
                .queryParam("client_id", context.getClient().getId().toString())
                .queryParam("redirect_uri", "http://valid.example.com/redirect")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("valid.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertTrue(params.containsKey("access_token"));
        assertEquals("Bearer", params.getFirst("token_type"));
        assertEquals(ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                Integer.valueOf(params.getFirst("expires_in")));
        assertEquals("debug", params.getFirst("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with a different, registered redirect works.
     */
    @Test
    public void testAuthorizeRedirectMulti() {
        // Register a new redirect on the current builder.
        context.redirect("http://redirect.example.com/redirect");

        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", context.getClient().getId().toString())
                .queryParam("redirect_uri",
                        "http://redirect.example.com/redirect")
                .request()
                .get();

        // Follow the redirect
        Response second = followRedirect(r);

        // Validate the redirect location
        URI location = second.getLocation();
        assertEquals("http", location.getScheme());
        assertEquals("redirect.example.com", location.getHost());
        assertEquals("/redirect", location.getPath());

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        assertTrue(params.containsKey("access_token"));
        assertEquals("Bearer", params.getFirst("token_type"));
        assertEquals(ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                Integer.valueOf(params.getFirst("expires_in")));
        assertFalse(params.containsKey("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that no registered redirect fails when a default redirect is
     * requested.
     */
    @Test
    public void testAuthorizeRedirectNoneRegistered() {
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", bareContext.getClient().getId())
                .request()
                .get();

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertNull(r.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_request", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that no registered redirect fails even when a redirect URI is
     * provided by the client.
     */
    @Test
    public void testAuthorizeRedirectNoneRegisteredWithRequest() {
        // Make sure the bare context has an authenticator.
        bareContext.authenticator("test");

        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id", bareContext.getClient().getId())
                .queryParam("redirect_uri",
                        "http://redirect.example.com/redirect")
                .request()
                .get();

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertNull(r.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
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
        // Register a new redirect on the current builder.
        context.redirect("http://redirect.example.com/redirect");

        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .request()
                .get();

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertNull(r.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
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
                        context.getClient().getId().toString())
                .request()
                .get();

        // We expect this response to head to /authorize/redirect
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, first.getStatus());
        URI firstLocation = first.getLocation();
        assertEquals("http", firstLocation.getScheme());
        assertEquals("localhost", firstLocation.getHost());
        assertEquals("/authorize/callback", firstLocation.getPath());

        // Follow the redirect
        Response second = followRedirect(first);
        URI secondLocation = second.getLocation();

        // Extract the query parameters in the fragment
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(secondLocation.getFragment());
        assertTrue(params.containsKey("access_token"));
        assertEquals("Bearer", params.getFirst("token_type"));
        assertEquals(ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                Integer.valueOf(params.getFirst("expires_in")));
        assertFalse(params.containsKey("scope"));
        assertFalse(params.containsKey("state"));
    }

    /**
     * Assert that a request with an invalid redirect fails.
     */
    @Test
    public void testAuthorizeRedirectInvalid() {
        Response r = target("/authorize")
                .queryParam("response_type", "token")
                .queryParam("client_id",
                        context.getClient().getId().toString())
                .queryParam("redirect_uri",
                        "http://invalid.example.com/redirect")
                .request()
                .get();

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertNull(r.getLocation());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the response parameters received.
        ErrorResponse error = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_request", error.getError());
        assertNotNull(error.getErrorDescription());
    }
}
