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

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test the list and filter methods of the scope service.
 *
 * @param <T> The type of entity to execute this test for.
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public abstract class AbstractServiceSearchTest<T extends AbstractEntity>
        extends AbstractResourceTest {

    /**
     * Class reference for this class' type, used in casting.
     */
    private final Class<T> typingClass;

    /**
     * The scope to grant the issued token.
     */
    private final String tokenScope;

    /**
     * The type of OAuth2 client to create.
     */
    private final ClientType clientType;

    /**
     * Whether to create a user, or fall back on an existing user. In most
     * cases, this will fall back to the owner of the admin scope.
     */
    private final Boolean createUser;

    /**
     * The token issued to the admin app, with appropriate credentials.
     */
    private OAuthToken adminAppToken;

    /**
     * An additional application context used for testing.
     */
    private EnvironmentBuilder otherApp;

    /**
     * Create a new instance of this parameterized test.
     *
     * @param typingClass The raw class type, used for type-based parsing.
     * @param clientType  The type of  client.
     * @param tokenScope  The client scope to issue.
     * @param createUser  Whether to create a new user.
     */
    public AbstractServiceSearchTest(final Class<T> typingClass,
                                     final ClientType clientType,
                                     final String tokenScope,
                                     final Boolean createUser) {
        this.typingClass = typingClass;
        this.tokenScope = tokenScope;
        this.clientType = clientType;
        this.createUser = createUser;
    }

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    protected abstract GenericType<List<T>> getListType();

    /**
     * Return the list of entities which should be accessible given a
     * specific token.
     *
     * @return The list of entities.
     */
    protected final List<T> getAccessibleEntities() {
        return getAccessibleEntities(getAdminToken());
    }

    /**
     * Return the list of entities which are owned by the current user.
     *
     * @return The list of entities.
     */
    protected final List<T> getOwnedEntities() {
        if (getAdminToken().getIdentity() == null) {
            return Collections.emptyList();
        } else {
            return getOwnedEntities(getAdminToken().getIdentity().getUser());
        }
    }

    /**
     * Return the list of entities which are owned by the provided token.
     *
     * @param token The token!
     * @return The list of entities.
     */
    protected final List<T> getOwnedEntities(final OAuthToken token) {
        if (token.getIdentity() == null) {
            return Collections.emptyList();
        } else {
            return getOwnedEntities(getAdminToken().getIdentity().getUser());
        }
    }

    /**
     * Return the list of entities which should be accessible given a
     * specific token.
     *
     * @param token The oauth token to test against.
     * @return A list of entities (could be empty).
     */
    protected abstract List<T> getAccessibleEntities(OAuthToken token);

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param owner The owner of these entities.
     * @return A list of entities (could be empty).
     */
    protected abstract List<T> getOwnedEntities(User owner);

    /**
     * Return the list of field names on which this particular entity type
     * has build a search index.
     *
     * @return An array of field names.
     */
    protected abstract String[] getSearchIndexFields();

    /**
     * Run a raw search query against our lucene index, given a specific string.
     *
     * @param query The search query to apply.
     * @return True if it's a member of a result set, otherwise false.
     */
    protected final List<T> getSearchResults(final String query) {
        QueryBuilder b = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(typingClass)
                .get();

        Query luceneQuery = b
                .keyword()
                .fuzzy()
                .onFields(getSearchIndexFields())
                .matching(query)
                .createQuery();

        List<T> results = (List<T>) getFullTextSession()
                .createFullTextQuery(luceneQuery, typingClass)
                .list();
        return results;
    }

    /**
     * Return the second application context (not the admin context).
     *
     * @return The secondary context in this test.
     */
    protected final EnvironmentBuilder getSecondaryContext() {
        return otherApp;
    }

    /**
     * Return the oauth token for the primary application.
     *
     * @return The application token.
     */
    protected final OAuthToken getAdminToken() {
        return adminAppToken;
    }

    /**
     * Return the oauth token for the secondary application.
     *
     * @return The application token.
     */
    protected final OAuthToken getSecondaryToken() {
        return otherApp.getToken();
    }

    /**
     * Return true if the current test parameters indicate a Client
     * Credentials-based client without any admin credentials. These types of
     * clients have no identity assigned to them, and therefore cannot 'own'
     * any resources which they could then access.
     *
     * @return True if this is a Client Credentials client without admin scope.
     */
    protected final Boolean isLimitedByClientCredentials() {
        return clientType.equals(ClientType.ClientCredentials)
                && tokenScope.equals(getRegularScope());
    }

    /**
     * Load data fixtures for each test. Here we're creating two applications
     * with different owners, using the kangaroo default scopes so we have
     * some good cross-app name duplication.
     *
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception An exception that indicates a failed fixture load.
     */
    @Override
    public final List<EnvironmentBuilder> fixtures(
            final EnvironmentBuilder adminApp) throws Exception {
        List<EnvironmentBuilder> fixtures = new ArrayList<>();

        // Get the admin app and create users based on the configured
        // parameters.
        EnvironmentBuilder context = getAdminContext().client(clientType);
        if (createUser) {
            context.user().identity();
        }
        adminAppToken = context.bearerToken(tokenScope).getToken();

        // Create a second app, owned by another user.
        otherApp = new EnvironmentBuilder(getSession())
                .owner(context.getUser())
                .scopes(Scope.allScopes());

        // Add some data for scopes
        context.scope("Single Scope");
        context.scope("Second Scope - many");
        context.scope("Third Scope - many");
        context.scope("Fourth Scope - many");
        otherApp.scope("Single Scope");
        otherApp.scope("Second Scope - many");
        otherApp.scope("Third Scope - many");
        otherApp.scope("Fourth Scope - many");

        // Add some test data for clients
        context.client(ClientType.ClientCredentials, "Single client");
        context.authenticator("Single authenticator");
        context.client(ClientType.ClientCredentials, "Second client - many");
        context.authenticator("Second authenticator - many");
        context.client(ClientType.Implicit, "Third client - many");
        context.authenticator("Third authenticator - many");
        context.client(ClientType.AuthorizationGrant, "Fourth client - many");
        context.authenticator("Fourth authenticator - many");
        otherApp.client(ClientType.ClientCredentials, "Single client");
        otherApp.authenticator("Single authenticator");
        otherApp.client(ClientType.ClientCredentials, "Second client - many");
        otherApp.authenticator("Second authenticator - many");
        otherApp.client(ClientType.Implicit, "Third client - many");
        otherApp.authenticator("Third authenticator - many");
        otherApp.client(ClientType.AuthorizationGrant, "Fourth client - many");
        otherApp.authenticator("Fourth authenticator - many");

        // Add some data for roles
        context.role("Single Role");
        context.role("Second Role - many");
        context.role("Third Role - many");
        context.role("Fourth Role - many");
        otherApp.role("Single Role");
        otherApp.role("Second Role - many");
        otherApp.role("Third Role - many");
        otherApp.role("Fourth Role - many");

        // Create a whole lot of applications to run some tests against.
        for (int i = 0; i < 10; i++) {
            String appName = String.format("Application %s- %s", i, i % 2 == 0
                    ? "many" : "frown");
            fixtures.add(new EnvironmentBuilder(getSession(), appName)
                    .owner(adminApp.getOwner()));
            fixtures.add(new EnvironmentBuilder(getSession(), appName)
                    .owner(adminApp.getUser()));
        }
        fixtures.add(new EnvironmentBuilder(getSession(), "Single")
                .owner(adminApp.getOwner()));
        fixtures.add(new EnvironmentBuilder(getSession(), "Single")
                .owner(adminApp.getUser()));

        // Create some users
        context.user()
                .identity()
                .claim("name", "Single User");
        context.user()
                .identity()
                .claim("name", "Second User - many");
        context.user()
                .identity()
                .claim("name", "Third User - many");
        context.user()
                .identity()
                .claim("name", "Fourth User - many");
        otherApp.user()
                .identity()
                .claim("name", "Single User");
        otherApp.user()
                .identity()
                .claim("name", "Second User - many");
        otherApp.user()
                .identity()
                .claim("name", "Third User - many");
        otherApp.user()
                .identity()
                .claim("name", "Fourth User - many");

        // Create a bunch of tokens
        context.redirect("http://single.token.example.com/")
                .authToken();
        context.redirect("http://second.token.example.com/many")
                .authToken();
        context.redirect("http://third.token.example.com/many")
                .authToken();
        context.redirect("http://fourth.token.example.com/many")
                .authToken();
        otherApp.redirect("http://single.token.example.com/")
                .authToken();
        otherApp.redirect("http://second.token.example.com/many")
                .authToken();
        otherApp.redirect("http://third.token.example.com/many")
                .authToken();
        otherApp.redirect("http://fourth.token.example.com/many")
                .authToken();

        fixtures.add(otherApp);

        return fixtures;
    }

    /**
     * Test that searching with no query throws an error.
     */
    @Test
    public final void testSearchNoQuery() {
        Map<String, String> params = new HashMap<>();

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test that searching with a stopword returns an error.
     */
    @Test
    public final void testSearchStopWord() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "with");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test a simple search.
     *
     * @throws Exception Unexpected exception.
     */
    @Test
    public final void testSearch() throws Exception {
        String query = "many";
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        Response r = search(params, token);

        // Determine result set.
        List<T> searchResults = getSearchResults(query);
        List<T> accessibleEntities = getAccessibleEntities(token);

        List<T> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            Assert.assertTrue(expectedTotal > 1);

            List<T> results = r.readEntity(getListType());
            Assert.assertEquals(expectedOffset.toString(),
                    r.getHeaderString("Offset"));
            Assert.assertEquals(expectedLimit.toString(),
                    r.getHeaderString("Limit"));
            Assert.assertEquals(expectedTotal.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(expectedResultSize, results.size());
        }
    }

    /**
     * Test that we can offset our search results.
     */
    @Test
    public final void testSearchOffset() {
        String query = "many";
        Integer offset = 1;
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("offset", offset.toString());
        Response r = search(params, token);

        // Determine result set.
        List<T> searchResults = getSearchResults(query);
        List<T> accessibleEntities = getAccessibleEntities(token);

        List<T> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal - offset);
        Integer expectedOffset = offset;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            Assert.assertTrue(expectedTotal > 1);

            List<T> results = r.readEntity(getListType());
            Assert.assertEquals(expectedOffset.toString(),
                    r.getHeaderString("Offset"));
            Assert.assertEquals(expectedLimit.toString(),
                    r.getHeaderString("Limit"));
            Assert.assertEquals(expectedTotal.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(expectedResultSize, results.size());
        }
    }

    /**
     * Test that we can limit our search results.
     */
    @Test
    public final void testSearchLimit() {
        String query = "many";
        Integer limit = 1;
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("limit", limit.toString());
        Response r = search(params, token);

        // Determine result set.
        List<T> searchResults = getSearchResults(query);
        List<T> accessibleEntities = getAccessibleEntities(token);

        List<T> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = limit;
        Integer expectedOffset = 0;
        Integer expectedLimit = limit;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            Assert.assertTrue(expectedTotal > 1);

            List<T> results = r.readEntity(getListType());
            Assert.assertEquals(expectedOffset.toString(),
                    r.getHeaderString("Offset"));
            Assert.assertEquals(expectedLimit.toString(),
                    r.getHeaderString("Limit"));
            Assert.assertEquals(expectedTotal.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(expectedResultSize, results.size());
        }
    }

    /**
     * Test that only an admin scope can search by a different user.
     */
    @Test
    public final void testSearchByOwner() {
        String query = "many";
        OAuthToken adminToken = getAdminToken();
        User secondaryOwner = getSecondaryContext().getOwner();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("owner", getAdminContext().getOwner().getId().toString());

        // Determine result set.
        List<T> searchResults = getSearchResults(query);
        List<T> ownedEntities = getOwnedEntities(secondaryOwner);
        List<T> accessibleEntities = getAccessibleEntities(adminToken);

        List<T> expectedResults = searchResults
                .stream()
                .filter((item) -> ownedEntities.indexOf(item) > -1)
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        // Make request.
        Response r = search(params, adminToken);

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(secondaryOwner, adminToken)) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            Assert.assertTrue(expectedTotal > 1);

            List<T> results = r.readEntity(getListType());
            Assert.assertEquals(200, r.getStatus());
            Assert.assertEquals(expectedTotal.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(expectedOffset.toString(),
                    r.getHeaderString("Offset"));
            Assert.assertEquals(expectedLimit.toString(),
                    r.getHeaderString("Limit"));
            Assert.assertEquals(expectedResultSize, results.size());
        }
    }

    /**
     * Test that any scope can search by their own owner ID.
     */
    @Test
    public final void testSearchBySelf() {
        String query = "single";
        OAuthToken adminToken = getAdminToken();
        if (adminToken.getIdentity() != null) {
            Map<String, String> params = new HashMap<>();
            params.put("q", query);
            params.put("owner", adminToken.getIdentity()
                    .getUser().getId().toString());

            // Determine result set.s
            List<T> searchResults = getSearchResults(query);
            List<T> ownedEntities = getOwnedEntities(adminToken);
            List<T> expectedResults = searchResults
                    .stream()
                    .filter((item) -> ownedEntities.indexOf(item) > -1)
                    .collect(Collectors.toList());
            Integer expectedCount = expectedResults.size();

            // Make request.
            Response r = search(params, adminToken);

            Assert.assertTrue(expectedCount > 0);

            List<T> results = r.readEntity(getListType());
            Assert.assertEquals(200, r.getStatus());
            Assert.assertEquals(expectedCount.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals("0", r.getHeaderString("Offset"));
            Assert.assertEquals("10", r.getHeaderString("Limit"));
            Assert.assertEquals(Math.min(10, expectedCount), results.size());
        } else {
            Assert.assertTrue(true);
        }
    }

    /**
     * Test that searches by an invalid owner.
     */
    // TODO(krotscheck): This should return a 400.
    @Test
    public final void testSearchByInvalidOwner() {
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("owner", UUID.randomUUID().toString());

        Response r = search(params, token);

        if (token.getScopes().keySet().contains(getAdminScope())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        }
    }

    /**
     * Test that searches by malformed owners returns a 404.
     */
    // TODO(krotscheck): This should return a 400.
    @Test
    public final void testSearchByMalformedOwner() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "single");
        params.put("owner", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Test that bad search results are empty.
     */
    @Test
    public final void testSearchNoResults() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "lolcat");
        Response r = search(params, getAdminToken());

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            List<T> results = r.readEntity(getListType());
            Assert.assertEquals("0", r.getHeaderString("Offset"));
            Assert.assertEquals("10", r.getHeaderString("Limit"));
            Assert.assertEquals("0", r.getHeaderString("Total"));
            Assert.assertEquals(0, results.size());
        }
    }

    /**
     * Assert that we can only browse the resource if we're logged in.
     */
    @Test
    public final void testSearchNoAuth() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "single");

        Response r = search(params, null);
        assertErrorResponse(r, Status.FORBIDDEN);
    }
}
