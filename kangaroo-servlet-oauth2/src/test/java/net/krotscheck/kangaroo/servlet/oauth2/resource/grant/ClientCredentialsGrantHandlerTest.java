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

package net.krotscheck.kangaroo.servlet.oauth2.resource.grant;

import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidGrantException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.UnauthorizedClientException;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientConfig;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.servlet.oauth2.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.test.DatabaseTest;
import net.krotscheck.kangaroo.test.IFixture;
import net.krotscheck.test.EnvironmentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * These tests ensure coverage on the Client Credentials token grant type
 * handler, covered in RFC6749.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.4">https://tools.ietf.org/html/rfc6749#section-4.4</a>
 */
public final class ClientCredentialsGrantHandlerTest extends DatabaseTest {

    /**
     * The harness under test.
     */
    private ClientCredentialsGrantHandler handler;

    /**
     * A simple, scoped context.
     */
    private EnvironmentBuilder context;

    /**
     * A non-client-credentials context.
     */
    private EnvironmentBuilder implicitContext;

    /**
     * A context with no configured scopes.
     */
    private EnvironmentBuilder noScopeContext;

    /**
     * A no-secret Context.
     */
    private EnvironmentBuilder noSecretContext;

    /**
     * Setup the test.
     */
    @Before
    public void initializeEnvironment() {
        handler = new ClientCredentialsGrantHandler(getSession());
    }

    /**
     * Set up the test harness data.
     */
    @Before
    public void createTestData() {
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        context = new EnvironmentBuilder(getSession())
                .client(ClientType.ClientCredentials, true)
                .scope("debug");
        noScopeContext = new EnvironmentBuilder(getSession())
                .client(ClientType.ClientCredentials, true);
        implicitContext = new EnvironmentBuilder(getSession())
                .client(ClientType.Implicit, true)
                .scope("debug");
        noSecretContext = new EnvironmentBuilder(getSession())
                .client(ClientType.ClientCredentials)
                .scope("debug");

        // The environment builder detaches its data, this reconnects it.
        getSession().refresh(context.getClient());

        List<IFixture> fixtures = new ArrayList<>();
        fixtures.add(context);
        fixtures.add(noScopeContext);
        fixtures.add(implicitContext);
        fixtures.add(noSecretContext);
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
     * Assert that a valid request works.
     */
    @Test
    public void testValidRequest() {
        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id",
                context.getClient().getId().toString());
        testData.putSingle("client_secret",
                context.getClient().getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "client_credentials");

        TokenResponseEntity token = handler.handle(context.getClient(),
                testData);
        Assert.assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        Assert.assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        Assert.assertNull(token.getRefreshToken());
        Assert.assertEquals("debug", token.getScope());
        Assert.assertNotNull(token.getAccessToken());
    }

    /**
     * Assert that requesting client_credentials against a
     * non-client_credentials client fails.
     */
    @Test(expected = InvalidGrantException.class)
    public void testInvalidClientType() {
        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id",
                implicitContext.getClient().getId().toString());
        testData.putSingle("client_secret",
                implicitContext.getClient().getClientSecret());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "client_credentials");

        handler.handle(implicitContext.getClient(), testData);
    }

    /**
     * Assert that requesting client_credentials with a client that has no
     * secret fails.
     */
    @Test(expected = UnauthorizedClientException.class)
    public void testNoClientSecret() {
        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id",
                noSecretContext.getClient().getId().toString());
        testData.putSingle("scope", "debug");
        testData.putSingle("grant_type", "client_credentials");

        handler.handle(noSecretContext.getClient(), testData);
    }

    /**
     * Assert that requesting a scope that is not permitted fails.
     */
    @Test(expected = InvalidScopeException.class)
    public void testInvalidScope() {
        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id",
                context.getClient().getId().toString());
        testData.putSingle("client_secret",
                context.getClient().getClientSecret());
        testData.putSingle("scope", "debug invalid");
        testData.putSingle("grant_type", "client_credentials");

        handler.handle(context.getClient(), testData);
    }

    /**
     * Assert that we can still make requests against an application with no
     * scopes.
     */
    @Test
    public void testNoScope() {
        Client c = noScopeContext.getClient();
        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id", c.getId().toString());
        testData.putSingle("client_secret", c.getClientSecret());
        testData.putSingle("scope", "");
        testData.putSingle("grant_type", "client_credentials");

        TokenResponseEntity token = handler.handle(c, testData);
        Assert.assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        Assert.assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        Assert.assertNull(token.getRefreshToken());
        Assert.assertNull(token.getScope());
        Assert.assertNotNull(token.getAccessToken());
    }
}
