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

package net.krotscheck.kangaroo.authz.oauth2.resource.grant;

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientConfig;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import net.krotscheck.kangaroo.test.jerseyTest.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

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
                            .client(ClientType.ClientCredentials, true)
                            .scope("debug")
                            .build();
                    noScopeContext = ApplicationBuilder.newApplication(session)
                            .client(ClientType.ClientCredentials, true)
                            .build();
                    implicitContext = ApplicationBuilder.newApplication(session)
                            .client(ClientType.Implicit, true)
                            .scope("debug")
                            .build();
                    noSecretContext = ApplicationBuilder.newApplication(session)
                            .client(ClientType.ClientCredentials)
                            .scope("debug")
                            .build();
                }
            };

    /**
     * The harness under test.
     */
    private static ClientCredentialsGrantHandler handler;

    /**
     * A simple, scoped context.
     */
    private static ApplicationContext context;

    /**
     * A non-client-credentials context.
     */
    private static ApplicationContext implicitContext;

    /**
     * A context with no configured scopes.
     */
    private static ApplicationContext noScopeContext;

    /**
     * A no-secret Context.
     */
    private static ApplicationContext noSecretContext;

    /**
     * Setup the test.
     */
    @Before
    public void initializeEnvironment() {
        handler = new ClientCredentialsGrantHandler(getSession());
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

        // Hydrate the client from the test session.
        Client testClient = getSession()
                .get(Client.class, context.getClient().getId());

        TokenResponseEntity token = handler.handle(testClient,
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
    @Test(expected = KangarooException.class)
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
    @Test(expected = KangarooException.class)
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
    @Test(expected = KangarooException.class)
    public void testInvalidScope() {
        MultivaluedMap<String, String> testData = new MultivaluedHashMap<>();
        testData.putSingle("client_id",
                context.getClient().getId().toString());
        testData.putSingle("client_secret",
                context.getClient().getClientSecret());
        testData.putSingle("scope", "debug invalid");
        testData.putSingle("grant_type", "client_credentials");

        // Hydrate the client from the test session.
        Client testClient = getSession()
                .get(Client.class, context.getClient().getId());

        handler.handle(testClient, testData);
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
