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

package net.krotscheck.kangaroo.authz.common.authenticator.github;

import net.krotscheck.kangaroo.authz.common.authenticator.exception.ThirdPartyErrorException;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2IdPToken;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.apache.commons.lang3.RandomUtils;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

import static net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType.Github;
import static net.krotscheck.kangaroo.authz.common.authenticator.oauth2.AbstractOAuth2Authenticator.CLIENT_ID_KEY;
import static net.krotscheck.kangaroo.authz.common.authenticator.oauth2.AbstractOAuth2Authenticator.CLIENT_SECRET_KEY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for the github authenticator.
 *
 * @author Michael Krotscheck
 */
public final class GithubAuthenticatorTest extends DatabaseTest {

    /**
     * DB Context, constructed for testing.
     */
    private static ApplicationContext context;

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
                    Map<String, String> githubConfig = new HashMap<>();
                    githubConfig.put(CLIENT_ID_KEY, "id");
                    githubConfig.put(CLIENT_SECRET_KEY, "secret");

                    context = ApplicationBuilder.newApplication(session)
                            .client(ClientType.AuthorizationGrant)
                            .role("some_role")
                            .authenticator(Github, githubConfig)
                            .build();
                }
            };

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
     * A mock get response.
     */
    private Response getResponse;

    /**
     * A mock client.
     */
    private GithubAuthenticator githubAuth;

    /**
     * Set up our mocks.
     */
    @Before
    public void bootstrap() {
        getSession().beginTransaction();

        this.client = mock(Client.class);
        this.webTarget = mock(WebTarget.class);
        this.builder = mock(Builder.class);
        this.getResponse = mock(Response.class);

        doReturn(webTarget).when(client).target(anyString());
        doReturn(builder).when(webTarget).request();
        doReturn(builder).when(builder).header(any(), any());
        doReturn(getResponse).when(builder).get();
        doReturn(Status.OK).when(getResponse).getStatusInfo();

        this.githubAuth = new GithubAuthenticator();
        this.githubAuth.setClient(client);
        this.githubAuth.setSession(getSession());
    }

    /**
     * Set up our mocks.
     */
    @After
    public void cleanup() {
        Transaction t = getSession().getTransaction();
        if (t.isActive()) {
            t.commit();
        }
    }

    /**
     * Test the various get() methods.
     */
    @Test
    public void testStaticAccessors() {
        assertEquals("https://github.com/login/oauth/authorize",
                githubAuth.getAuthEndpoint());
        assertEquals("https://github.com/login/oauth/access_token",
                githubAuth.getTokenEndpoint());
        assertEquals("read:user,user:email", githubAuth.getScopes());
    }

    /**
     * Test a basic user load.
     */
    @Test
    public void testLoadUser() {
        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("github_access_token");

        GithubUserEntity user = new GithubUserEntity();
        user.setId(RandomUtils.nextInt());
        user.setName("test name");

        doReturn(user).when(getResponse).readEntity(GithubUserEntity.class);

        OAuth2User returnedUser = githubAuth.loadUserIdentity(result);
        assertEquals(returnedUser.getId(), user.getId().toString());
        assertEquals("test name", returnedUser.getClaims().get("name"));
    }

    /**
     * Assert that if the user response returns with an error, it's recast
     * to the client.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testLoadUserWithRemoteError() {
        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("github_access_token");

        Map<String, String> response = new HashMap<>();
        response.put("error", "test");
        response.put("error_description", "description");

        doReturn(Status.BAD_REQUEST).when(getResponse).getStatusInfo();
        doReturn(response).when(getResponse)
                .readEntity(GithubAuthenticator.MAP_TYPE);

        githubAuth.loadUserIdentity(result);
    }

    /**
     * Assert that if the user response returns with an unparseable
     * response, it throws a simple error.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testLoadUserUnparseable() {
        OAuth2IdPToken result = new OAuth2IdPToken();
        result.setAccessToken("github_access_token");

        doThrow(ProcessingException.class)
                .when(getResponse).readEntity(GithubUserEntity.class);

        githubAuth.loadUserIdentity(result);
    }

    /**
     * Assert that if the user response returns with no remote id, throw it
     * back to the user.
     */
    @Test(expected = ThirdPartyErrorException.class)
    public void testLoadUserNoResponse() {
        OAuth2IdPToken idPToken = new OAuth2IdPToken();
        idPToken.setAccessToken("github_access_token");

        GithubUserEntity result = new GithubUserEntity(); // no id
        doReturn(result).when(getResponse).readEntity(GithubUserEntity.class);

        githubAuth.loadUserIdentity(idPToken);
    }

    /**
     * If we cannot close the user client... assume that it's an uncast error,
     * caught by the generic 5xx handler.
     */
    @Test(expected = Exception.class)
    public void testLoadUserErrorOnClose() {
        OAuth2IdPToken idPToken = new OAuth2IdPToken();
        idPToken.setAccessToken("github_access_token");

        GithubUserEntity testUser = new GithubUserEntity();
        testUser.setId(RandomUtils.nextInt());
        testUser.setName("Some Random Name");
        testUser.setEmail("lol@example.com");
        doReturn(testUser).when(getResponse)
                .readEntity(GithubUserEntity.class);

        doThrow(Exception.class).when(getResponse).close();

        MultivaluedStringMap params = new MultivaluedStringMap();
        params.putSingle("code", "valid_code");
        githubAuth.loadUserIdentity(idPToken);
    }
}
