/*
 * Copyright (c) 2017 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.common.authenticator.oauth2;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.MisconfiguredAuthenticatorException;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.ThirdPartyErrorException;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidRequestException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.ServerErrorException;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the oauth2 authenticator.
 *
 * @author Michael Krotscheck
 */
public class AbstractOAuth2AuthenticatorTest
        extends DatabaseTest {

    /**
     * DB Context, constructed for testing.
     */
    private static ApplicationContext context;

    /**
     * DB Context, constructed for testing.
     */
    private static ApplicationContext mirrorContext;

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
                    Map<String, String> fbConfig = new HashMap<>();
                    fbConfig.put(AbstractOAuth2Authenticator.CLIENT_ID_KEY,
                            "id");
                    fbConfig.put(AbstractOAuth2Authenticator.CLIENT_SECRET_KEY,
                            "secret");

                    context = ApplicationBuilder.newApplication(session)
                            .client(ClientType.AuthorizationGrant)
                            .role("some_role")
                            .authenticator(AuthenticatorType.Test, fbConfig)
                            .build();

                    mirrorContext = ApplicationBuilder.newApplication(session)
                            .client(ClientType.AuthorizationGrant)
                            .role("some_role")
                            .authenticator(AuthenticatorType.Test, fbConfig)
                            .build();
                }
            };
    /**
     * The authenticator under test.
     */
    private TestOAuth2Authenticator authenticator;

    /**
     * A test authenticator configuration.
     */
    private Authenticator config;

    /**
     * A valid callback.
     */
    private URI validCallback = URI.create("http://example"
            + ".com/authorize/callback?state=state");

    /**
     * A mock client.
     */
    private Client client;

    /**
     * A mock web target.
     */
    private WebTarget webTarget;

    /**
     * A mock invocation builder.
     */
    private Builder builder;

    /**
     * A mock post response.
     */
    private Response postResponse;

    /**
     * A mock get response.
     */
    private Response getResponse;

    /**
     * Setup the tests.
     */
    @Before
    public void setup() {
        this.authenticator = new TestOAuth2Authenticator();
    }

    /**
     * Set up our mocks.
     */
    @Before
    public void bootstrap() {
        getSession().beginTransaction();
        config = context.getAuthenticator();

        this.client = mock(Client.class);
        this.webTarget = mock(WebTarget.class);
        this.builder = mock(Builder.class);
        this.getResponse = mock(Response.class);
        this.postResponse = mock(Response.class);

        doReturn(webTarget).when(client).target(anyString());
        doReturn(builder).when(webTarget).request();
        doReturn(builder).when(builder).header(any(), any());
        doReturn(getResponse).when(builder).get();
        doReturn(postResponse).when(builder).post(any());
        doReturn(Status.OK).when(getResponse).getStatusInfo();
        doReturn(Status.OK).when(postResponse).getStatusInfo();

        this.authenticator = new TestOAuth2Authenticator();
        this.authenticator.setClient(client);
        this.authenticator.setSession(getSession());
    }

    /**
     * Tear down our mocks.
     */
    @After
    public void cleanup() {
        Transaction t = getSession().getTransaction();
        if (t.isActive()) {
            t.commit();
        }
    }

    /**
     * Test getting and setting the session.
     */
    @Test
    public void testGetSetSession() {
        assertNotNull(authenticator.getSession());
        authenticator.setSession(null);
        assertNull(authenticator.getSession());
        authenticator.setSession(getSession());
        assertNotNull(authenticator.getSession());
    }

    /**
     * Test getting and setting the client.
     */
    @Test
    public void testGetSetClient() {
        assertNotNull(authenticator.getClient());
        authenticator.setClient(null);
        assertNull(authenticator.getClient());
        authenticator.setClient(client);
        assertNotNull(authenticator.getClient());
    }

    /**
     * Valid requests should redirect to facebook.
     */
    @Test
    public void testDelegate() {
        Response r = authenticator.delegate(config, validCallback);

        assertEquals(302, r.getStatus());
        String location = r.getHeaderString(HttpHeaders.LOCATION);
        URI redirect = URI.create(location);
        assertEquals("example.com", redirect.getHost());
        assertEquals("/authorize", redirect.getPath());
        assertEquals("http", redirect.getScheme());

        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(redirect);
        assertEquals("id", params.getFirst("client_id"));
        assertEquals("test scope", params.getFirst("scope"));
        assertEquals("code", params.getFirst("response_type"));
        assertEquals("http://example.com/authorize/callback",
                params.getFirst("redirect_uri"));
        assertEquals("state", params.getFirst("state"));
    }

    /**
     * If an authenticator is invalid for some reason, this needs to be sent
     * back to the user as a generic misconfigured exception.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testDelegateInvalidConfiguration() {
        Authenticator testConfig = new Authenticator();
        authenticator.delegate(testConfig, validCallback);
    }

    /**
     * If delegate() is called with no callback, throw an error. This
     * shouldn't happen, so it should be a 500.
     */
    @Test(expected = ServerErrorException.class)
    public void testDelegateNoCallback() {
        authenticator.delegate(config, null);
    }

    /**
     * If delegate() is called with a callback with no state, throw an error.
     * This shouldn't happen, so it should be a 500.
     */
    @Test(expected = ServerErrorException.class)
    public void testDelegateNoCallbackState() {
        URI callback = URI.create("http://example.com/authorize/callback");
        authenticator.delegate(config, callback);
    }

    /**
     * Assert that a simple authentication response, with a valid response
     * from facebook, returns expected values.
     */
    @Test
    public void testAuthenticate() {
        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("facebook_access_token");
        doReturn(result).when(postResponse).readEntity(OAuth2IdPToken.class);

        OAuth2User testUser = new OAuth2User();
        testUser.setId(RandomStringUtils.randomAlphanumeric(10));
        testUser.getClaims().put("name", "Some Random Name");
        testUser.getClaims().put("email", "lol@example.com");
        doReturn(testUser).when(getResponse)
                .readEntity(OAuth2User.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        UserIdentity identity =
                authenticator.authenticate(config, params, validCallback);

        assertEquals(testUser.getId(), identity.getRemoteId());
        assertEquals("Some Random Name", testUser.getClaims().get("name"));
        assertEquals("lol@example.com", testUser.getClaims().get("email"));
    }

    /**
     * Assert that if the flow is started with an error response, it's recast to
     * the client.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testAuthenticateWithRemoteError() {
        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("error", "test");
        params.putSingle("error_description", "description");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * Assert that if the flow is started with no authorization code, it throws
     * it back to the client.
     */
    @Test(expected = InvalidRequestException.class)
    public void testAuthenticateWithNoAuthCode() {
        MultivaluedStringMap params = new MultivaluedStringMap();
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * Assert that if the token response returns with an error, it's recast
     * to the client.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testAuthenticateWithTokenError() {
        Map<String, String> response = new HashMap<>();
        response.put("error", "test");
        response.put("error_description", "description");

        doReturn(Status.BAD_REQUEST).when(postResponse).getStatusInfo();
        doReturn(response).when(postResponse)
                .readEntity(AbstractOAuth2Authenticator.MAP_TYPE);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * Assert that if the token response returns with an unparseable
     * response, it throws a simple error.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testAuthenticateWithUnparseableToken() {
        doThrow(ProcessingException.class)
                .when(postResponse).readEntity(OAuth2IdPToken.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * Assert that if - for some reason - facebook doesn't return a token, we
     * throw that error back to the client.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testAuthenticateWithNoTokenResponse() {
        OAuth2IdPToken result = new OAuth2IdPToken();
        doReturn(result).when(postResponse).readEntity(OAuth2IdPToken.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * If we cannot close the token client... assume that it's an uncast error,
     * caught by the generic 5xx handler.
     */
    @Test(expected = Exception.class)
    public void testAuthenticateErrorOnTokenClose() {
        OAuth2IdPToken idPToken = new OAuth2IdPToken();
        idPToken.setAccessToken("facebook_access_token");
        doReturn(idPToken).when(postResponse)
                .readEntity(OAuth2IdPToken.class);

        doThrow(Exception.class).when(postResponse).close();

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * Assert that if the user response returns with an error, it's recast
     * to the client.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testAuthenticateWithUserError() {
        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("facebook_access_token");
        doReturn(result).when(postResponse).readEntity(OAuth2IdPToken.class);

        Map<String, String> response = new HashMap<>();
        response.put("error", "test");
        response.put("error_description", "description");

        doReturn(Status.BAD_REQUEST).when(getResponse).getStatusInfo();
        doReturn(response).when(getResponse)
                .readEntity(AbstractOAuth2Authenticator.MAP_TYPE);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * Assert that if the user response returns with an unparseable
     * response, it throws a simple error.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testAuthenticateWithUnparseableUser() {
        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("facebook_access_token");
        doReturn(result).when(postResponse).readEntity(OAuth2IdPToken.class);

        doThrow(ProcessingException.class)
                .when(getResponse).readEntity(OAuth2User.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * Assert that if the user response returns with no remote id, throw it
     * back to the user.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testAuthenticateWithNoUserResponse() {
        OAuth2IdPToken idPToken = new OAuth2IdPToken();
        idPToken.setAccessToken("facebook_access_token");
        doReturn(idPToken).when(postResponse)
                .readEntity(OAuth2IdPToken.class);

        OAuth2User result = new OAuth2User(); // no id
        doReturn(result).when(getResponse).readEntity(OAuth2User.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * If we cannot close the user client... assume that it's an uncast error,
     * caught by the generic 5xx handler.
     */
    @Test(expected = Exception.class)
    public void testAuthenticateErrorOnClose() {
        OAuth2IdPToken idPToken = new OAuth2IdPToken();
        idPToken.setAccessToken("facebook_access_token");
        doReturn(idPToken).when(postResponse)
                .readEntity(OAuth2IdPToken.class);

        OAuth2User testUser = new OAuth2User();
        testUser.setId(RandomStringUtils.randomAlphanumeric(10));
        testUser.getClaims().put("name", "Some Random Name");
        testUser.getClaims().put("email", "lol@example.com");
        doReturn(testUser).when(getResponse)
                .readEntity(OAuth2User.class);

        doThrow(Exception.class).when(getResponse).close();

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        authenticator.authenticate(config, params, validCallback);
    }

    /**
     * Assert that a new user is created if the identity is not found for the
     * present application.
     */
    @Test
    public void testAuthenticateCreateNewUser() {
        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("facebook_access_token");
        doReturn(result).when(postResponse).readEntity(OAuth2IdPToken.class);

        OAuth2User testUser = new OAuth2User();
        testUser.setId(RandomStringUtils.randomAlphanumeric(10));
        testUser.getClaims().put("name", "Some Random Name");
        testUser.getClaims().put("email", "lol@example.com");
        doReturn(testUser).when(getResponse)
                .readEntity(OAuth2User.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        UserIdentity identity =
                authenticator.authenticate(config, params, validCallback);
        // Simulate a request completion
        getSession().getTransaction().commit();
        getSession().refresh(identity.getUser());

        assertEquals(1, identity.getUser().getIdentities().size());
        assertEquals("lol@example.com",
                identity.getClaims().get("email"));
        assertEquals("Some Random Name",
                identity.getClaims().get("name"));
    }

    /**
     * Assert that a user's identity claims are updated, if the identity is
     * found for the present application.
     */
    @Test
    public void testAuthenticateUpdateExistingUser() {
        ApplicationContext testContext = context.getBuilder()
                .user()
                .identity("remote_identity")
                .build();

        assertEquals(0, testContext.getUserIdentity().getClaims().size());

        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("facebook_access_token");
        doReturn(result).when(postResponse).readEntity(OAuth2IdPToken.class);

        OAuth2User testUser = new OAuth2User();
        testUser.setId("remote_identity");
        testUser.getClaims().put("name", "Some Random Name");
        testUser.getClaims().put("email", "lol@example.com");
        doReturn(testUser).when(getResponse)
                .readEntity(OAuth2User.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        UserIdentity identity =
                authenticator.authenticate(config, params, validCallback);
        assertEquals(identity, testContext.getUserIdentity());
        assertEquals("lol@example.com",
                identity.getClaims().get("email"));
        assertEquals("Some Random Name",
                identity.getClaims().get("name"));
    }

    /**
     * Assert that multiple identical user identities, assigned to different
     * applications, do not conflict on the creation of a new identity.
     */
    @Test
    public void testAuthenticateNewUserNoConflict() {
        String remoteIdentity = RandomStringUtils.randomAlphabetic(10);
        ApplicationContext testContext = mirrorContext.getBuilder()
                .user()
                .identity(remoteIdentity)
                .claim("email", "email@example.com")
                .build();

        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("facebook_access_token");
        doReturn(result).when(postResponse).readEntity(OAuth2IdPToken.class);

        OAuth2User testUser = new OAuth2User();
        testUser.setId(remoteIdentity);
        testUser.getClaims().put("name", "Some Random Name");
        testUser.getClaims().put("email", "lol@example.com");
        doReturn(testUser).when(getResponse)
                .readEntity(OAuth2User.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        UserIdentity identity =
                authenticator.authenticate(config, params, validCallback);
        // Simulate a request completion
        getSession().getTransaction().commit();
        getSession().refresh(identity.getUser());

        assertEquals(remoteIdentity, identity.getRemoteId());
        assertEquals("lol@example.com",
                identity.getClaims().get("email"));
        assertEquals("Some Random Name",
                identity.getClaims().get("name"));

        assertNotEquals(identity, testContext.getUserIdentity());

        getSession().refresh(testContext.getUserIdentity());
        getSession().refresh(identity);
        assertNotEquals(
                testContext.getUserIdentity().getClaims().get("email"),
                identity.getClaims().get("email"));
    }

    /**
     * Assert that multiple identical user identities, assigned to different
     * applications, do not conflict on the update of an existing identity.
     */
    @Test
    public void testAuthenticateUpdateUserNoConflict() {
        String remoteIdentity = RandomStringUtils.randomAlphabetic(10);
        ApplicationContext testContext = context.getBuilder()
                .user()
                .identity(remoteIdentity)
                .claim("email", "email@example.com")
                .build();
        ApplicationContext testMirrorContext = mirrorContext.getBuilder()
                .user()
                .identity(remoteIdentity)
                .claim("email", "email@example.com")
                .build();

        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("facebook_access_token");
        doReturn(result).when(postResponse).readEntity(OAuth2IdPToken.class);

        OAuth2User testUser = new OAuth2User();
        testUser.setId(remoteIdentity);
        testUser.getClaims().put("name", "Some Random Name");
        testUser.getClaims().put("email", "lol@example.com");
        doReturn(testUser).when(getResponse)
                .readEntity(OAuth2User.class);

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        UserIdentity identity =
                authenticator.authenticate(config, params, validCallback);
        // Simulate a request completion
        getSession().getTransaction().commit();

        // Refresh all our entities
        getSession().refresh(identity.getUser());
        getSession().refresh(testContext.getUserIdentity());
        getSession().refresh(testMirrorContext.getUserIdentity());

        assertEquals(remoteIdentity, identity.getRemoteId());
        assertEquals("lol@example.com",
                identity.getClaims().get("email"));
        assertEquals("Some Random Name",
                identity.getClaims().get("name"));

        assertNotEquals(identity, testMirrorContext.getUserIdentity());
        assertEquals("email@example.com",
                testMirrorContext.getUserIdentity().getClaims().get("email"));
    }

    /**
     * Assert that passing in a null authenticator fails.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testValidateNullInput() throws Exception {
        authenticator.validate(null);
    }

    /**
     * Assert that passing in a null configuration fails.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testValidateNullConfig() throws Exception {
        Authenticator config = new Authenticator();
        config.setConfiguration(null);

        authenticator.validate(config);
    }

    /**
     * Assert that passing in an empty configuration fails.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testValidateEmptyConfig() throws Exception {
        Authenticator config = new Authenticator();
        authenticator.validate(config);
    }

    /**
     * Assert that passing in a config with no app id fails.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testValidateNoAppId() throws Exception {
        Authenticator config = new Authenticator();
        config.getConfiguration()
                .put(AbstractOAuth2Authenticator.CLIENT_SECRET_KEY, "foo");

        authenticator.validate(config);
    }

    /**
     * Assert that passing in a config with no app secret fails.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = MisconfiguredAuthenticatorException.class)
    public void testValidateNoAppSecret() throws Exception {
        Authenticator config = new Authenticator();
        config.getConfiguration()
                .put(AbstractOAuth2Authenticator.CLIENT_ID_KEY, "foo");
        authenticator.validate(config);
    }

    /**
     * Assert that passing in a valid config passes.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testValidate() throws Exception {
        Authenticator config = new Authenticator();
        config.getConfiguration()
                .put(AbstractOAuth2Authenticator.CLIENT_ID_KEY, "foo");
        config.getConfiguration()
                .put(AbstractOAuth2Authenticator.CLIENT_SECRET_KEY, "bar");

        authenticator.validate(config);
    }

    /**
     * Our test authenticator.
     */
    private static final class TestOAuth2Authenticator
            extends AbstractOAuth2Authenticator {

        /**
         * Test authorization endpoint.
         *
         * @return Test endpoint.
         */
        @Override
        protected String getAuthEndpoint() {
            return "http://example.com/authorize";
        }

        /**
         * The test token endpoint.
         *
         * @return Test endpoint.
         */
        @Override
        protected String getTokenEndpoint() {
            return "http://example.com/token";
        }

        /**
         * List of required test scopes.
         *
         * @return Test scopes.
         */
        @Override
        protected String getScopes() {
            return "test scope";
        }

        /**
         * Mock user identity loading.
         *
         * @param token The OAuth token.
         * @return A user identity.
         */
        @Override
        protected OAuth2User loadUserIdentity(final OAuth2IdPToken token) {
            Response r = getClient()
                    .target("http://example.com/user")
                    .request()
                    .header(HttpHeaders.AUTHORIZATION,
                            HttpUtil.authHeaderBearer(token.getAccessToken()))
                    .get();

            try {
                // If this is an error...
                if (r.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                    return r.readEntity(OAuth2User.class);
                } else {
                    Map<String, String> params = r.readEntity(MAP_TYPE);
                    throw new ThirdPartyErrorException(params);
                }
            } catch (ProcessingException e) {
                throw new ThirdPartyErrorException();
            } finally {
                r.close();
            }
        }
    }
}
