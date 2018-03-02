/*
 * Copyright (c) 2018 Michael Krotscheck
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
 *
 */

package net.krotscheck.kangaroo.authz.oauth2.rfc7009;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.oauth2.OAuthAPI;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.hibernate.id.MalformedIdException;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import net.krotscheck.kangaroo.test.jersey.SingletonTestContainerFactory;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import net.krotscheck.kangaroo.test.runner.SingleInstanceTestRunner;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static net.krotscheck.kangaroo.util.HttpUtil.authHeaderBasic;
import static net.krotscheck.kangaroo.util.HttpUtil.authHeaderBearer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * These unit tests handle the token revocation endpoint.
 */
@RunWith(SingleInstanceTestRunner.class)
public class TokenRevocationTest extends ContainerTest {

    /**
     * The test context for a regular application.
     */
    private static ApplicationContext context;

    /**
     * The test context for a peer application.
     */
    private static ApplicationContext otherContext;

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
                            .client(ClientType.ClientCredentials, true)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://www.example.com/")
                            .user()
                            .identity()
                            .claim("one", "claim")
                            .claim("two", "claim")
                            .bearerToken()
                            .build();
                    otherContext = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .scope("debug1")
                            .role("test", new String[]{"debug"})
                            .client(ClientType.AuthorizationGrant, true)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://www.example.com/")
                            .user()
                            .identity()
                            .claim("red", "claim")
                            .claim("blue", "claim")
                            .build();
                }
            };
    /**
     * Test container factory.
     */
    private SingletonTestContainerFactory testContainerFactory;

    /**
     * The current running test application.
     */
    private ResourceConfig testApplication;

    /**
     * This method overrides the underlying default test container provider,
     * with one that provides a singleton instance. This allows us to
     * circumvent the often expensive initialization routines that come from
     * bootstrapping our services.
     *
     * @return an instance of {@link TestContainerFactory} class.
     * @throws TestContainerException if the initialization of
     *                                {@link TestContainerFactory} instance
     *                                is not successful.
     */
    protected TestContainerFactory getTestContainerFactory()
            throws TestContainerException {
        if (this.testContainerFactory == null) {
            this.testContainerFactory =
                    new SingletonTestContainerFactory(
                            super.getTestContainerFactory(),
                            this.getClass());
        }
        return testContainerFactory;
    }

    /**
     * Create the application under test.
     *
     * @return A configured api servlet.
     */
    @Override
    protected ResourceConfig createApplication() {
        if (testApplication == null) {
            testApplication = new OAuthAPI();
        }
        return testApplication;
    }

    /**
     * Convert a map into an HTML form payload.
     *
     * @param values The values to map.
     * @return The payload.
     */
    private Entity buildEntity(final Map<String, String> values) {
        Form f = new Form();
        values.forEach(f::param);
        return Entity.entity(f, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }

    /**
     * Assert that the revocation response matches the expected response
     * body, and that the revoked token no longer exists in the database.
     *
     * @param r            The response.
     * @param revokedToken The revoked token.
     */
    private void assertValidRevocation(final Response r,
                                       final OAuthToken revokedToken) {
        assertEquals(205, r.getStatus());
//        assertFalse(r.hasEntity());

        Session s = getSession();
        s.evict(revokedToken);
        s.beginTransaction();
        OAuthToken existingToken = s.get(OAuthToken.class,
                revokedToken.getId());
        s.getTransaction().commit();
        assertNull(existingToken);
    }

    /**
     * Assert that a bearer can revoke itself.
     */
    @Test
    public void testRevokeSelf() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(bearerToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, bearerToken);
    }

    /**
     * Assert that a bearer token can revoke another bearer token if it's
     * attached to the same user.
     */
    @Test
    public void testRevokeOtherBearerSameUser() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken revokedToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that a bearer token cannot revoke a bearer token that belongs
     * to a different user.
     */
    @Test
    public void testRevokeOtherBearerDifferentUser() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        testContext = otherContext.getBuilder()
                .user()
                .identity()
                .bearerToken("debug")
                .build();
        OAuthToken revokedToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that a bearer cannot revoke a malformed token.
     */
    @Test
    public void testRevokeMalformed() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", "malformed_token");

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new MalformedIdException());
    }

    /**
     * Assert that a bearer cannot revoke a nonexistent token.
     */
    @Test
    public void testRevokeNonexistent() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(IdUtil.next()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that a bearer token can revoke its own refresh token.
     */
    @Test
    public void testRevokeOwnRefresh() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        testContext = testContext.getBuilder()
                .refreshToken()
                .build();
        OAuthToken refreshToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(refreshToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, refreshToken);
    }

    /**
     * Assert that a bearer token can revoke another refresh token if it's
     * attached to the same user.
     */
    @Test
    public void testRevokeOtherRefreshSameUser() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that a bearer token cannot revoke another bearer token if it's
     * owned by a different user.
     */
    @Test
    public void testRevokeOtherRefreshDifferentUser() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        testContext = otherContext.getBuilder()
                .user()
                .identity()
                .bearerToken("debug")
                .refreshToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that a bearer token can revoke another authorization code if it's
     * attached to the same user.
     */
    @Test
    public void testRevokeOtherAuthorizationCodeSameUser() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        testContext = otherContext.getBuilder()
                .authToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that a bearer token cannot revoke another bearer token if it's
     * owned by a different user.
     */
    @Test
    public void testRevokeOtherAuthorizationCodeDifferentUser() {
        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken bearerToken = testContext.getToken();
        testContext = otherContext.getBuilder()
                .user()
                .identity()
                .authToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();
        String header = authHeaderBearer(bearerToken.getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that client credentials can revoke a bearer token issued to the
     * same client.
     */
    @Test
    public void testRevokeBearerBySameClient() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that client credentials cannot revoke a malformed token.
     */
    @Test
    public void testRevokeMalformedByClient() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        Map<String, String> values = new HashMap<>();
        values.put("token", "malformed_token");

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new MalformedIdException());
    }

    /**
     * Assert that client credentials cannot revoke a nonexistent token.
     */
    @Test
    public void testRevokeInvalidByClient() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(IdUtil.next()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that client credentials can revoke a bearer token issued to a
     * different client in the same application.
     */
    @Test
    public void testRevokeBearerByOtherClient() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.Implicit)
                .bearerToken("debug")
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that client credentials can revoke a refresh token issued to the
     * same client.
     */
    @Test
    public void testRevokeRefreshBySameClient() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that client credentials can revoke a refresh token issued to a
     * different client in the same application.
     */
    @Test
    public void testRevokeRefreshByOtherClient() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.Implicit)
                .bearerToken("debug")
                .refreshToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that client credentials can revoke an authorization code issued to
     * the same client.
     */
    @Test
    public void testRevokeAuthCodeBySameClient() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = context.getBuilder()
                .authToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that authorization code can revoke an authorization code issued
     * to a different client in the same application.
     */
    @Test
    public void testRevokeAuthCodeByOtherClient() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.Implicit)
                .authToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }


    /**
     * Assert that authorization code can revoke a bearer token issued to the
     * same client.
     */
    @Test
    public void testRevokeBearerBySamePrivateClient() {
        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .bearerToken("debug")
                .build();
        Client c = testContext.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that authorization code cannot revoke a malformed token.
     */
    @Test
    public void testRevokeMalformedByPrivateClient() {
        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .build();
        Client c = testContext.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        Map<String, String> values = new HashMap<>();
        values.put("token", "malformed_token");

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new MalformedIdException());
    }

    /**
     * Assert that authorization code cannot revoke a nonexistent token.
     */
    @Test
    public void testRevokeInvalidByPrivateClient() {
        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .build();
        Client c = testContext.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(IdUtil.next()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that a token issued to a client credentials client can revoke a
     * bearer token issued to the same client.
     */
    @Test
    public void testRevokeBearerBySameClientToken() {
        Client c = context.getClient();
        String header = authHeaderBearer(context.getToken().getId());

        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that a token issued to a client credentials client cannot revoke
     * a malformed token.
     */
    @Test
    public void testRevokeMalformedByClientToken() {
        String header = authHeaderBearer(context.getToken().getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", "malformed_token");

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new MalformedIdException());
    }

    /**
     * Assert that a token issued to a client credentials client cannot revoke
     * a nonexistent token.
     */
    @Test
    public void testRevokeInvalidByClientToken() {
        Client c = context.getClient();
        String header = authHeaderBearer(context.getToken().getId());

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(IdUtil.next()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that a token issued to a client credentials client can revoke
     * a bearer token issued to a different client in the same application.
     */
    @Test
    public void testRevokeBearerByOtherClientToken() {
        Client c = context.getClient();
        String header = authHeaderBearer(context.getToken().getId());

        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.Implicit)
                .bearerToken("debug")
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that a token issued to a client credentials client can revoke
     * a refresh token issued to the same client.
     */
    @Test
    public void testRevokeRefreshBySameClientToken() {
        Client c = context.getClient();
        String header = authHeaderBearer(context.getToken().getId());

        ApplicationContext testContext = context.getBuilder()
                .bearerToken("debug")
                .refreshToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that a token issued to a client credentials client can revoke
     * a refresh token issued to a different client in the same application.
     */
    @Test
    public void testRevokeRefreshByOtherClientToken() {
        Client c = context.getClient();
        String header = authHeaderBearer(context.getToken().getId());

        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.Implicit)
                .bearerToken("debug")
                .refreshToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that a token issued to a client credentials client can revoke
     * an authorization code issued to the same client.
     */
    @Test
    public void testRevokeAuthCodeBySameClientToken() {
        Client c = context.getClient();
        String header = authHeaderBearer(context.getToken().getId());

        ApplicationContext testContext = context.getBuilder()
                .authToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that a token issued to a client credentials client can revoke
     * can revoke an authorization code issued to a different client in the
     * same application.
     */
    @Test
    public void testRevokeAuthCodeByOtherClientToken() {
        Client c = context.getClient();
        String header = authHeaderBearer(context.getToken().getId());

        ApplicationContext testContext = context.getBuilder()
                .client(ClientType.Implicit)
                .authToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertValidRevocation(r, revokedToken);
    }

    /**
     * Assert that client credentials cannot revoke a refresh token issued to
     * a different application.
     */
    @Test
    public void testRevokeRefreshByDifferentApplication() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken()
                .refreshToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that client credentials cannot revoke a Bearer token issued to
     * a different application.
     */
    @Test
    public void testRevokeBearerByDifferentApplication() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = otherContext.getBuilder()
                .bearerToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }

    /**
     * Assert that client credentials cannot revoke a auth code issued to
     * a different application.
     */
    @Test
    public void testRevokeAuthCodeByDifferentApplication() {
        Client c = context.getClient();
        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        ApplicationContext testContext = otherContext.getBuilder()
                .authToken()
                .build();
        OAuthToken revokedToken = testContext.getToken();

        Map<String, String> values = new HashMap<>();
        values.put("token", IdUtil.toString(revokedToken.getId()));

        Response r = target("/revoke")
                .request()
                .header(AUTHORIZATION, header)
                .post(buildEntity(values));

        assertErrorResponse(r, new NotFoundException());
    }
}
