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
 *
 */

package net.krotscheck.kangaroo.test;

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.AuthenticatorState;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.database.entity.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Test the environment builder.
 *
 * @author Michael Krotscheck
 */
public final class EnvironmentBuilderTest extends DatabaseTest {

    /**
     * Fixture context for the environment builder.
     */
    private EnvironmentBuilder context;

    /**
     * Load data fixtures for each test.
     *
     * @param session The session to wra
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<EnvironmentBuilder> fixtures(final Session session) {
        // Create an independent session.
        SessionFactory f = getSessionFactory();

        context = new EnvironmentBuilder(session, "Test App")
                .client(ClientType.OwnerCredentials)
                .authenticator("password")
                .scopes(Arrays.asList("one", "two", "three"))
                .user();

        return Arrays.asList(context);
    }

    /**
     * Helper method, makes sure a new entity has been created with all
     * default values.
     *
     * @param e The entity to check.
     */
    private void assertNewResource(final AbstractEntity e) {
        Assert.assertNotNull(e);
        Assert.assertNotNull(e.getId());
        Assert.assertNotNull(e.getCreatedDate());
        Assert.assertNotNull(e.getModifiedDate());
    }

    /**
     * A quick smoketest that goes through all the methods in the
     * EnvironmentBuilder.
     *
     * @throws Exception Should not be thrown.
     */
    @SuppressWarnings("methodlength")
    @Test
    public void smokeTest() throws Exception {
        EnvironmentBuilder b = new EnvironmentBuilder(getSession());

        // Make sure we have an application.
        Assert.assertNotNull(b.getApplication());

        // Set the owner
        Assert.assertNull(b.getOwner());
        b.owner(context.getUser());
        Assert.assertEquals(context.getUser(), b.getOwner());

        // Create a scope
        Assert.assertNull(b.getScope());
        b.scope("testScope");
        assertNewResource(b.getScope());
        Assert.assertEquals("testScope", b.getScope().getName());
        Assert.assertEquals(b.getApplication(), b.getScope().getApplication());

        // Create a role.
        Assert.assertNull(b.getRole());
        b.role("testRole");
        assertNewResource(b.getRole());
        Assert.assertEquals("testRole", b.getRole().getName());
        Assert.assertEquals(b.getApplication(), b.getRole().getApplication());
        Assert.assertEquals(0, b.getRole().getScopes().size());

        // Create a role with scopes.
        b.role("scopedRole", new String[]{"testScope", "omgNoScope"});
        assertNewResource(b.getRole());
        Assert.assertEquals("scopedRole", b.getRole().getName());
        Assert.assertEquals(b.getApplication(), b.getRole().getApplication());
        Assert.assertEquals(1, b.getRole().getScopes().size());

        // Add more scopes
        b.scopes(Arrays.asList("otherScope", "yetAnotherScope"));
        Assert.assertEquals("yetAnotherScope", b.getScope().getName());
        Assert.assertEquals(3, b.getScopes().size());

        // Create a new client
        Assert.assertNull(b.getClient());
        b.client(ClientType.AuthorizationGrant);
        assertNewResource(b.getClient());
        Assert.assertEquals(ClientType.AuthorizationGrant,
                b.getClient().getType());
        Assert.assertNull(b.getClient().getClientSecret());
        Assert.assertEquals(b.getApplication(), b.getClient().getApplication());

        // Create a new private client
        b.client(ClientType.AuthorizationGrant, true);
        assertNewResource(b.getClient());
        Assert.assertEquals(ClientType.AuthorizationGrant,
                b.getClient().getType());
        Assert.assertNotNull(b.getClient().getClientSecret());
        Assert.assertEquals(b.getApplication(), b.getClient().getApplication());

        // Create a named client
        b.client(ClientType.AuthorizationGrant, "new name");
        assertNewResource(b.getClient());
        Assert.assertEquals(ClientType.AuthorizationGrant,
                b.getClient().getType());
        Assert.assertEquals("new name", b.getClient().getName());

        // Set up a test authenticator for this client.
        Assert.assertNull(b.getAuthenticator());
        b.authenticator("test");
        assertNewResource(b.getAuthenticator());
        Assert.assertEquals("test", b.getAuthenticator().getType());
        Assert.assertEquals(b.getClient(), b.getAuthenticator().getClient());

        // Add a redirect for this client.
        Assert.assertEquals(0, b.getClient().getRedirects().size());
        b.redirect("http://example.com");
        Assert.assertNotNull(b.getClient().getRedirects());
        Assert.assertTrue(b.getClient().getRedirects()
                .contains(new URI("http://example.com")));
        Assert.assertEquals(1, b.getClient().getRedirects().size());

        // Add another redirect
        b.redirect("http://example.com/two");
        Assert.assertEquals(2, b.getClient().getRedirects().size());

        // Add a referrer for this client.
        Assert.assertEquals(0, b.getClient().getReferrers().size());
        b.referrer("http://example.com");
        Assert.assertNotNull(b.getClient().getReferrers());
        Assert.assertTrue(b.getClient().getReferrers()
                .contains(new URI("http://example.com")));
        Assert.assertEquals(1, b.getClient().getReferrers().size());

        // Add another referrer
        b.referrer("http://example.com/two");
        Assert.assertEquals(2, b.getClient().getReferrers().size());

        // Add a user to this application.
        Assert.assertNull(b.getUser());
        b.user();
        assertNewResource(b.getUser());

        // Add a user identity with a login.
        Assert.assertNull(b.getUserIdentity());
        b.login("login", "password");
        assertNewResource(b.getUserIdentity());
        Assert.assertEquals("login", b.getUserIdentity().getRemoteId());
        Assert.assertNotNull(b.getUserIdentity().getPassword());

        // Add a random user identity.
        b.identity();
        assertNewResource(b.getUserIdentity());
        Assert.assertNotEquals("login", b.getUserIdentity().getRemoteId());

        // Add a regular user identity to the current authenticator
        b.identity("remote_identity");
        assertNewResource(b.getUserIdentity());
        Assert.assertEquals("remote_identity",
                b.getUserIdentity().getRemoteId());
        Assert.assertNull(b.getUserIdentity().getPassword());
        Assert.assertEquals(b.getAuthenticator(),
                b.getUserIdentity().getAuthenticator());

        // Add some claims to the user identity
        Assert.assertEquals(0, b.getUserIdentity().getClaims().size());
        b.claim("foo", "bar");
        b.claim("lol", "cat");
        Assert.assertEquals("bar", b.getUserIdentity().getClaims().get("foo"));
        Assert.assertEquals("cat", b.getUserIdentity().getClaims().get("lol"));

        // Create an auth token
        Assert.assertNull(b.getToken());
        b.authToken();
        assertNewResource(b.getToken());
        OAuthToken aToken = b.getToken();
        Assert.assertEquals(OAuthTokenType.Authorization,
                aToken.getTokenType());
        Assert.assertEquals(b.getClient(), aToken.getClient());
        Assert.assertEquals(b.getUserIdentity(), aToken.getIdentity());
        Assert.assertFalse(aToken.isExpired());
        Assert.assertNull(aToken.getAuthToken());

        // Create an auth token
        b.bearerToken();
        assertNewResource(b.getToken());
        OAuthToken bToken = b.getToken();
        Assert.assertEquals(OAuthTokenType.Bearer, bToken.getTokenType());
        Assert.assertEquals(b.getClient(), bToken.getClient());
        Assert.assertEquals(b.getUserIdentity(), bToken.getIdentity());
        Assert.assertFalse(bToken.isExpired());
        Assert.assertNull(bToken.getAuthToken());

        // Create a refresh token
        b.refreshToken();
        assertNewResource(b.getToken());
        OAuthToken rToken = b.getToken();
        Assert.assertEquals(OAuthTokenType.Refresh, rToken.getTokenType());
        Assert.assertEquals(b.getClient(), rToken.getClient());
        Assert.assertEquals(b.getUserIdentity(), rToken.getIdentity());
        Assert.assertFalse(rToken.isExpired());
        Assert.assertEquals(bToken, rToken.getAuthToken());

        // Create an expired token
        b.token(OAuthTokenType.Bearer, true, null, null, null);
        assertNewResource(b.getToken());
        OAuthToken eToken = b.getToken();
        Assert.assertEquals(OAuthTokenType.Bearer, eToken.getTokenType());
        Assert.assertEquals(b.getClient(), eToken.getClient());
        Assert.assertEquals(b.getUserIdentity(), eToken.getIdentity());
        Assert.assertTrue(eToken.isExpired());

        // Create a scoped token
        b.token(OAuthTokenType.Bearer, false, "testScope", null, null);
        OAuthToken sToken = b.getToken();
        assertNewResource(sToken);
        Assert.assertEquals(OAuthTokenType.Bearer, sToken.getTokenType());
        Assert.assertEquals(b.getClient(), sToken.getClient());
        Assert.assertEquals(b.getUserIdentity(), sToken.getIdentity());
        Assert.assertTrue(sToken.getScopes().containsKey("testScope"));
        Assert.assertFalse(sToken.isExpired());

        // Create a token for a credentials client.
        b.client(ClientType.ClientCredentials)
                .bearerToken();
        OAuthToken cToken = b.getToken();
        assertNewResource(cToken);
        Assert.assertEquals(OAuthTokenType.Bearer, cToken.getTokenType());
        Assert.assertEquals(b.getClient(), cToken.getClient());
        Assert.assertNull(cToken.getIdentity());

        // Create an authenticator state
        Assert.assertNull(b.getAuthenticatorState());
        b.authenticatorState();
        AuthenticatorState aState = b.getAuthenticatorState();
        assertNewResource(aState);
        Assert.assertEquals(b.getClient(), aState.getClient());
        Assert.assertEquals(b.getAuthenticator(), aState.getAuthenticator());

        // Delete one of our entities to make sure it's skipped during clearing.
        Transaction t = getSession().beginTransaction();
        getSession().delete(b.getToken());
        t.commit();

        // Assert that everything clears.
        b.clear();
        Assert.assertNull(b.getApplication());
        Assert.assertNull(b.getClient());
        Assert.assertNull(b.getRole());
        Assert.assertNull(b.getUser());
        Assert.assertNull(b.getUserIdentity());
        Assert.assertNull(b.getScope());
        Assert.assertEquals(0, b.getScopes().size());
        Assert.assertNull(b.getAuthenticator());
        Assert.assertNull(b.getToken());
    }

    /**
     * Assert that we can wrap the builder around an application.
     */
    @Test
    public void testApplicationWrapper() {
        // A mock, self-owned admin application.
        Application adminApp = context.getApplication();

        Session session = getSession();
        EnvironmentBuilder b =
                new EnvironmentBuilder(session, context.getApplication());
        Assert.assertEquals(adminApp, b.getApplication());

        // Create a user in the new context.
        b.user();
        User createdUser = b.getUser();

        b.clear();

        // Assert that entities are still persisted, even though the context
        // builder's been cleared.
        User oldUser = session.get(User.class, createdUser.getId());
        Assert.assertNotNull(oldUser);

        adminApp = session.get(Application.class, adminApp.getId());
        Assert.assertNotNull(adminApp);
    }

    /**
     * Assert that we can wrap the builder around an application.
     */
    @Test
    public void testApplicationWrapperWithRoles() {
        // Add some roles...
        context.role("admin");
        context.role("member");

        // A mock, self-owned admin application.
        Application adminApp = context.getApplication();

        Session session = getSession();
        EnvironmentBuilder b =
                new EnvironmentBuilder(session, context.getApplication());
        Assert.assertEquals(adminApp, b.getApplication());
    }

    /**
     * Assert that we can wrap the builder around an application.
     */
    @Test
    public void testApplicationWrapperWithIdentities() {
        // Add an identity
        context.identity("admin");

        // A mock, self-owned admin application.
        Application adminApp = context.getApplication();

        Session session = getSession();
        EnvironmentBuilder b =
                new EnvironmentBuilder(session, context.getApplication());
        Assert.assertEquals(adminApp, b.getApplication());
        Assert.assertNotNull(b.getUserIdentity());
    }

    /**
     * Test that a managed application can be deleted by an API call, without
     * causing a major problem.
     */
    @Test
    public void testDeleteEnvironmentApplication() {
        SessionFactory f = getSessionFactory();
        Session builderSession = f.openSession();

        // Create a dummy owner...
        context.user();

        // Create a new application with some basic data.
        EnvironmentBuilder b = new EnvironmentBuilder(builderSession)
                .owner(context.getUser())
                .client(ClientType.OwnerCredentials)
                .authenticator("password")
                .scope("test")
                .role("admin")
                .user();

        // Create a new session
        Session manualSession = f.openSession();
        Assert.assertNotSame(manualSession, builderSession);

        // Load the app into this session.
        Application testApp = manualSession.get(Application.class,
                b.getApplication().getId());
        Assert.assertNotNull(testApp);

        // Delete the app
        Transaction t = manualSession.beginTransaction();
        manualSession.delete(testApp);
        t.commit();
        manualSession.close();

        // Now clear the builder.
        b.clear();
        builderSession.close();
    }
}
