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

package net.krotscheck.kangaroo.servlet.oauth2.resource;

import net.krotscheck.kangaroo.servlet.oauth2.OAuthTestApp;
import net.krotscheck.kangaroo.database.entity.AuthenticatorState;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.HttpUtil;
import net.krotscheck.kangaroo.test.IFixture;
import net.krotscheck.test.EnvironmentBuilder;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.test.TestProperties;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Unit tests for the authorization endpoint. Note that this is not a
 * comprehensive suite, as it only covers edge cases and functionality not
 * otherwise covered in our RFC test suites.
 *
 * @author Michael Krotscheck
 */
public final class AuthorizationServiceTest extends ContainerTest {

    /**
     * Simple testing context.
     */
    private EnvironmentBuilder context;

    /**
     * An owner context.
     */
    private EnvironmentBuilder ownerContext;

    /**
     * Build and configure the application.
     *
     * @return A configured application.
     */
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new OAuthTestApp();
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        context = new EnvironmentBuilder(getSession())
                .client(ClientType.Implicit)
                .authenticator("foo")
                .redirect("http://valid.example.com/redirect");


        ownerContext = new EnvironmentBuilder(getSession())
                .client(ClientType.OwnerCredentials)
                .authenticator("test")
                .redirect("http://valid.example.com/redirect");

        List<IFixture> fixtures = new ArrayList<>();
        fixtures.add(context);
        fixtures.add(ownerContext);
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
     * Assert that an invalid grant type is rejected.
     */
    @Test
    public void testInvalidResponseType() {
        Response r = target("/authorize")
                .queryParam("response_type", "invalid")
                .queryParam("client_id", context.getClient().getId())
                .request()
                .get();

        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());

        URI location = r.getLocation();
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertEquals("unsupported_response_type",
                params.getFirst("error"));
        Assert.assertNotNull(params.get("error_description"));
    }

    /**
     * Assert that an valid grant type is accepted.
     */
    @Test
    public void testValidResponseType() {
        Response r = target("/authorize")
                .queryParam("response_type", "test")
                .queryParam("client_id", context.getClient().getId())
                .request()
                .get();

        URI location = r.getLocation();
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertEquals("unsupported_response_type",
                params.getFirst("error"));
        Assert.assertNotNull(params.get("error_description"));
    }

    /**
     * Assert that an valid grant type is accepted.
     */
    @Test
    public void testCallbackMalformedStateId() {
        Response r = target("/authorize/callback")
                .queryParam("state", "not_a_parseable_uuid")
                .request()
                .get();

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        ErrorResponse e = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", e.getError());
        Assert.assertNotNull(e.getErrorDescription());
    }

    /**
     * Assert that an valid grant type is accepted.
     */
    @Test
    public void testCallbackInvalidStateId() {
        Response r = target("/authorize/callback")
                .queryParam("state", UUID.randomUUID().toString())
                .request()
                .get();

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        ErrorResponse e = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", e.getError());
        Assert.assertNotNull(e.getErrorDescription());
    }

    /**
     * Test against an unimplemented authenticator. This test should
     * technically never be triggered, but we're checking it anyway.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCallbackUnimplementedAuthenticator() throws Exception {
        AuthenticatorState state = new AuthenticatorState();
        state.setClient(context.getClient());
        state.setAuthenticator(context.getAuthenticator());
        state.setClientRedirect(new URI("http://valid.example.com/redirect"));

        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.save(state);
        t.commit();

        Response r = target("/authorize/callback")
                .queryParam("state", state.getId().toString())
                .request()
                .get();

        URI location = r.getLocation();
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertEquals("invalid_request", params.getFirst("error"));
        Assert.assertNotNull(params.get("error_description"));
    }

    /**
     * If, for some inexplicable reason, an AuthenticatorState was created
     * linked to a non-implicit or authorization-grant client, we should make
     * sure that the request errors.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCallbackStateWithInvalidClientType() throws Exception {

        AuthenticatorState state = new AuthenticatorState();
        state.setClient(ownerContext.getClient());
        state.setAuthenticator(ownerContext.getAuthenticator());
        state.setClientRedirect(new URI("http://valid.example.com/redirect"));

        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.save(state);
        t.commit();

        Response r = target("/authorize/callback")
                .queryParam("state", state.getId().toString())
                .request()
                .get();

        URI location = r.getLocation();
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getQuery());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertEquals("invalid_request", params.getFirst("error"));
        Assert.assertNotNull(params.get("error_description"));
    }
}
