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

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for subresource browsing.
 *
 * @param <T> The type of entity to execute this test for.
 * @author Michael Krotscheck
 */
@Deprecated
@RunWith(Parameterized.class)
public abstract class DAbstractSubserviceBrowseTest<K extends AbstractEntity,
        T extends AbstractEntity> extends DAbstractResourceTest {

    /**
     * The scope to grant the issued token.
     */
    private final String tokenScope;

    /**
     * The type of OAuth2 client to create.
     */
    private final ClientType clientType;

    /**
     * The client under test.
     */
    private Client client;

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
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public DAbstractSubserviceBrowseTest(final ClientType clientType,
                                         final String tokenScope,
                                         final Boolean createUser) {
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
     * specific token and parent entity.
     *
     * @param parentEntity The parent entity to filter on.
     * @param token        The oauth token to test against.
     * @return A list of entities (could be empty).
     */
    protected abstract List<T> getAccessibleEntities(K parentEntity,
                                                     OAuthToken token);

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param parentEntity The parent entity to filter on.
     * @param owner        The owner of these entities.
     * @return A list of entities (could be empty).
     */
    protected abstract List<T> getOwnedEntities(K parentEntity, User owner);

    /**
     * Return the list of entities which should be accessible given a
     * specific token.
     *
     * @param parentEntity The parent entity to filter on.
     * @return The list of entities.
     */
    protected final List<T> getAccessibleEntities(final K parentEntity) {
        return getAccessibleEntities(parentEntity, getAdminToken());
    }

    /**
     * Return the list of entities which are owned by the current user.
     *
     * @param parentEntity The parent entity to filter on.
     * @return The list of entities.
     */
    protected final List<T> getOwnedEntities(final K parentEntity) {
        if (getAdminToken().getIdentity() == null) {
            return Collections.emptyList();
        } else {
            return getOwnedEntities(parentEntity,
                    getAdminToken().getIdentity().getUser());
        }
    }

    /**
     * Return the list of entities which are owned by the provided token.
     *
     * @param parentEntity The parent entity to filter on.
     * @param token        The token!
     * @return The list of entities.
     */
    protected final List<T> getOwnedEntities(final K parentEntity,
                                             final OAuthToken token) {
        if (token.getIdentity() == null) {
            return Collections.emptyList();
        } else {
            return getOwnedEntities(parentEntity,
                    getAdminToken().getIdentity().getUser());
        }
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
     * Return the client under test.
     *
     * @return The client under test.
     */
    public final Client getAdminClient() {
        return client;
    }

    /**
     * Retrieve the scope under test.
     *
     * @return The scope that's being tested.
     */
    public final String getTokenScope() {
        return tokenScope;
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
     * Return the correct parent entity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    protected abstract K getParentEntity(EnvironmentBuilder context);

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
        EnvironmentBuilder context = getAdminContext()
                .client(clientType);

        client = context.getClient();

        // Create a whole lot of applications to run some tests against.
        for (int i = 0; i < 10; i++) {
            String appName = String.format("Application %s- %s", i, i % 2 == 0
                    ? "many" : "frown");
            fixtures.add(new EnvironmentBuilder(getSession(), appName)
                    .owner(adminApp.getUser()));
        }
        fixtures.add(new EnvironmentBuilder(getSession(), "Single")
                .owner(adminApp.getUser()));


        if (createUser) {
            context.user().identity();
        }
        adminAppToken = context.bearerToken(tokenScope).getToken();

        // Create a whole lot more applications to run some tests against.
        for (int i = 0; i < 10; i++) {
            String appName = String.format("Application %s- %s", i, i % 2 == 0
                    ? "many" : "frown");
            fixtures.add(new EnvironmentBuilder(getSession(), appName)
                    .owner(adminApp.getUser()));
        }
        fixtures.add(new EnvironmentBuilder(getSession(), "Single")
                .owner(adminApp.getUser()));

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

        // Create a bunch of referrers
        context.referrer("http://single.referrer.example.com/");
        context.referrer("http://second.referrer.example.com/many");
        context.referrer("http://third.referrer.example.com/many");
        context.referrer("http://fourth.referrer.example.com/many");
        otherApp.referrer("http://single.referrer.example.com/");
        otherApp.referrer("http://second.referrer.example.com/many");
        otherApp.referrer("http://third.referrer.example.com/many");
        otherApp.referrer("http://fourth.referrer.example.com/many");

        fixtures.add(otherApp);
        return fixtures;
    }

    /**
     * Assert that you can browse applications.
     */
    @Test
    public final void testBrowse() {
        Map<String, String> params = new HashMap<>();
        Response r = browse(params, adminAppToken);

        K parentEntity = getParentEntity(getAdminContext());
        Integer expectedResults = getAccessibleEntities(parentEntity,
                adminAppToken).size();

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else if (!isAccessible(parentEntity, adminAppToken)) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else {
            List<T> results = r.readEntity(getListType());
            Assert.assertEquals("0", r.getHeaderString("Offset"));
            Assert.assertEquals("10", r.getHeaderString("Limit"));
            Assert.assertEquals(expectedResults.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(Math.min(expectedResults, 10), results.size());
        }
    }

    /**
     * Assert that you can browse with a limit set.
     */
    @Test
    public final void testBrowseLimit() {
        Integer limit = 2;
        Map<String, String> params = new HashMap<>();
        params.put("limit", limit.toString());

        K parentEntity = getParentEntity(getAdminContext());
        Response r = browse(params, adminAppToken);
        Integer expectedResults =
                getAccessibleEntities(parentEntity, adminAppToken).size();

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else if (!isAccessible(parentEntity, adminAppToken)) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else {
            List<T> results = r.readEntity(getListType());
            Assert.assertEquals("0", r.getHeaderString("Offset"));
            Assert.assertEquals(limit.toString(), r.getHeaderString("Limit"));
            Assert.assertEquals(expectedResults.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(Math.min(expectedResults, limit),
                    results.size());
        }
    }

    /**
     * Assert that you can browse with an offset.
     */
    @Test
    public final void testBrowseOffset() {
        Integer offset = 1;
        Map<String, String> params = new HashMap<>();
        params.put("offset", offset.toString());

        K parentEntity = getParentEntity(getAdminContext());
        Response r = browse(params, adminAppToken);
        Integer expectedResults = getAccessibleEntities(parentEntity,
                adminAppToken).size();

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else if (!isAccessible(parentEntity, adminAppToken)) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else {
            List<T> results = r.readEntity(getListType());
            Assert.assertEquals(offset.toString(),
                    r.getHeaderString("Offset"));
            Assert.assertEquals("10", r.getHeaderString("Limit"));
            Assert.assertEquals(expectedResults.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(Math.min(expectedResults - offset, 10),
                    results.size());
        }
    }

    /**
     * Assert that you can browse and sort default ascending.
     */
    @Test
    public final void testBrowseSortDefault() {
        Map<String, String> params = new HashMap<>();
        params.put(ApiParam.SORT_QUERY, "createdDate");

        K parentEntity = getParentEntity(getAdminContext());
        Response r = browse(params, adminAppToken);
        Integer expectedResults = getAccessibleEntities(parentEntity,
                adminAppToken).size();

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else if (!isAccessible(parentEntity, adminAppToken)) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else {
            List<T> results = r.readEntity(getListType());
            Assert.assertEquals("0", r.getHeaderString("Offset"));
            Assert.assertEquals("10", r.getHeaderString("Limit"));
            Assert.assertEquals(expectedResults.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(Math.min(expectedResults, 10),
                    results.size());

            results.stream().sorted((e1, e2) -> e1.getCreatedDate()
                    .compareTo(e2.getCreatedDate()));
        }
    }

    /**
     * Assert that you can browse and sort ascending.
     */
    @Test
    public final void testBrowseSortAscending() {
        Map<String, String> params = new HashMap<>();
        params.put(ApiParam.SORT_QUERY, "createdDate");
        params.put(ApiParam.ORDER_QUERY, "ASC");

        K parentEntity = getParentEntity(getAdminContext());
        Response r = browse(params, adminAppToken);
        Integer expectedResults = getAccessibleEntities(parentEntity,
                adminAppToken).size();

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else if (!isAccessible(parentEntity, adminAppToken)) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else {
            List<T> results = r.readEntity(getListType());
            Assert.assertEquals("0", r.getHeaderString("Offset"));
            Assert.assertEquals("10", r.getHeaderString("Limit"));
            Assert.assertEquals(expectedResults.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(Math.min(expectedResults, 10),
                    results.size());

            results.stream().sorted(
                    Comparator.comparing(AbstractEntity::getCreatedDate));
        }
    }

    /**
     * Assert that you can browse and sort descending.
     */
    @Test
    public final void testBrowseSortDescending() {
        Map<String, String> params = new HashMap<>();
        params.put(ApiParam.SORT_QUERY, "createdDate");
        params.put(ApiParam.ORDER_QUERY, "DESC");

        Response r = browse(params, adminAppToken);
        K parentEntity = getParentEntity(getAdminContext());
        Integer expectedResults = getAccessibleEntities(parentEntity,
                adminAppToken).size();

        if (clientType.equals(ClientType.ClientCredentials)
                && tokenScope.equals(getRegularScope())) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else if (!isAccessible(parentEntity, adminAppToken)) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else {
            List<T> results = r.readEntity(getListType());
            Assert.assertEquals("0", r.getHeaderString("Offset"));
            Assert.assertEquals("10", r.getHeaderString("Limit"));
            Assert.assertEquals(expectedResults.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(Math.min(expectedResults, 10),
                    results.size());

            results.stream().sorted((e1, e2) -> e2.getCreatedDate()
                    .compareTo(e1.getCreatedDate()));
        }
    }

    /**
     * Assert that an invalid sort parameter defaults to ASC.
     */
    @Test
    public final void testBrowseSortInvalid() {
        Map<String, String> params = new HashMap<>();
        params.put(ApiParam.SORT_QUERY, "createdDate");
        params.put(ApiParam.ORDER_QUERY, "invalid_sort");

        Response r = browse(params, adminAppToken);
        K parentEntity = getParentEntity(getAdminContext());
        Integer expectedResults = getAccessibleEntities(parentEntity,
                adminAppToken).size();

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else if (!isAccessible(parentEntity, adminAppToken)) {
            assertErrorResponse(r, Status.NOT_FOUND.getStatusCode(),
                    "not_found");
        } else {
            List<T> results = r.readEntity(getListType());
            Assert.assertEquals("0", r.getHeaderString("Offset"));
            Assert.assertEquals("10", r.getHeaderString("Limit"));
            Assert.assertEquals(expectedResults.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(Math.min(expectedResults, 10),
                    results.size());

            results.stream().sorted((e1, e2) -> e1.getCreatedDate()
                    .compareTo(e2.getCreatedDate()));
        }
    }

    /**
     * Assert that we can only browse the resource if we're logged in.
     */
    @Test
    public final void testBrowseNoAuth() {
        Response response = target(getBrowseUrl().getPath())
                .request()
                .get();

        assertErrorResponse(response, Status.FORBIDDEN);
    }
}
