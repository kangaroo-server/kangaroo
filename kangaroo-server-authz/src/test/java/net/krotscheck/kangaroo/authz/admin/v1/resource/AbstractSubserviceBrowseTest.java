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

package net.krotscheck.kangaroo.authz.admin.v1.resource;

import net.krotscheck.kangaroo.authz.admin.v1.test.SingletonTestContainerFactory;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.runner.ParameterizedSingleInstanceTestRunner.ParameterizedSingleInstanceTestRunnerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for subresource browsing.
 *
 * @param <K> The type of the parent entity.
 * @param <T> The type of entity to execute this test for.
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
@UseParametersRunnerFactory(ParameterizedSingleInstanceTestRunnerFactory.class)
public abstract class AbstractSubserviceBrowseTest<K extends AbstractAuthzEntity,
        T extends AbstractAuthzEntity> extends AbstractResourceTest {

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
    private ApplicationContext otherApp;

    /**
     * Test container factory.
     */
    private SingletonTestContainerFactory testContainerFactory;

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public AbstractSubserviceBrowseTest(final ClientType clientType,
                                        final String tokenScope,
                                        final Boolean createUser) {
        this.tokenScope = tokenScope;
        this.clientType = clientType;
        this.createUser = createUser;
    }

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
     * Load data fixtures for each test. Here we're creating two applications
     * with different owners, using the kangaroo default scopes so we have
     * some good cross-app name duplication.
     *
     * @throws Exception An exception that indicates a failed fixture load.
     */
    @Before
    public final void configureData() throws Exception {

        // Get the admin app and create users based on the configured
        // parameters.
        ApplicationContext context = getAdminContext();
        User owner = context.getOwner();

        client = context.getBuilder()
                .client(clientType)
                .build()
                .getClient();

        if (createUser) {
            // Switch to the other user.
            owner = getSecondaryContext().getOwner();
        }
        UserIdentity identity = owner.getIdentities().iterator().next();

        adminAppToken = context
                .getBuilder()
                .bearerToken(client, identity, tokenScope)
                .build()
                .getToken();
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
    protected abstract K getParentEntity(ApplicationContext context);

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
                    Comparator.comparing(AbstractAuthzEntity::getCreatedDate));
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

        assertErrorResponse(response, Status.UNAUTHORIZED);
    }
}
