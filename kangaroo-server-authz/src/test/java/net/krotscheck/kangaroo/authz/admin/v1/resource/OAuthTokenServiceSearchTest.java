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

import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test the search endpoint of the token service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class OAuthTokenServiceSearchTest
        extends AbstractServiceSearchTest<OAuthToken> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<OAuthToken>> LIST_TYPE =
            new GenericType<ListResponseEntity<OAuthToken>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public OAuthTokenServiceSearchTest(final ClientType clientType,
                                       final String tokenScope,
                                       final Boolean createUser) {
        super(OAuthToken.class, clientType, tokenScope, createUser);
    }

    /**
     * Test parameters.
     *
     * @return The parameters passed to this test during every run.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.TOKEN_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.TOKEN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.TOKEN_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.TOKEN,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.TOKEN_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.TOKEN,
                        false
                });
    }

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<OAuthToken>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the list of entities which are owned by the given user.
     * Includes scope checks.
     *
     * @param owner The owner of the entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<OAuthToken> getOwnedEntities(final User owner) {
        // Get all the owned clients.
        return getAttached(owner).getApplications()
                .stream()
                .flatMap(a -> a.getClients().stream())
                .flatMap(c -> c.getTokens().stream())
                .collect(Collectors.toList());
    }

    /**
     * Return the list of field names on which this particular entity type
     * has build a search index.
     *
     * @return An array of field names.
     */
    protected String[] getSearchIndexFields() {
        return new String[]{"identity.remoteId", "identity.claims"};
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.TOKEN_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.TOKEN;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return UriBuilder.fromPath("/token/")
                .path(id)
                .build();
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractAuthzEntity entity) {
        return getUrlForId(IdUtil.toString(entity.getId()));
    }

    /**
     * Test that we can filter a search by an user ID.
     */
    @Test
    public void testSearchByUser() {
        // Find a search term with some results.
        Application app = getSecondaryContext().getApplication();
        String query = "many";
        List<OAuthToken> searchResults = getSearchResults(query);
        User user = searchResults.stream()
                .filter(t -> t.getClient().getApplication().equals(app))
                .map(OAuthToken::getIdentity)
                .map(UserIdentity::getUser)
                .collect(Collectors.toList())
                .get(0);

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("user", IdUtil.toString(user.getId()));
        Response r = search(params, token);

        // Determine result set.
        List<OAuthToken> accessibleEntities = getAccessibleEntities(token);
        List<OAuthToken> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getIdentity() != null)
                .filter((item) -> item.getIdentity().getUser().equals(user))
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(user, token)) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            Assert.assertTrue(expectedTotal > 0);

            assertListResponse(r,
                    expectedResultSize,
                    expectedOffset,
                    expectedLimit,
                    expectedTotal);
        }
    }

    /**
     * Test that an invalid user throws an error.
     */
    @Test
    public void testSearchByInvalidUser() {
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("user", IdUtil.toString(IdUtil.next()));

        Response r = search(params, token);
        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Test that an malformed user throws an error.
     */
    // TODO(krotscheck): This should return a 400.
    @Test
    public void testSearchByMalformedUser() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("user", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Test that we can filter a search by an identity ID.
     */
    @Test
    public void testSearchByIdentity() {
        // Find a search term with some results.
        Application app = getSecondaryContext().getApplication();
        String query = "many";
        List<OAuthToken> searchResults = getSearchResults(query);
        UserIdentity identity = searchResults.stream()
                .filter(t -> t.getClient().getApplication().equals(app))
                .map(OAuthToken::getIdentity)
                .collect(Collectors.toList())
                .get(0);

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("identity", IdUtil.toString(identity.getId()));
        Response r = search(params, token);

        // Determine result set.
        List<OAuthToken> accessibleEntities = getAccessibleEntities(token);
        List<OAuthToken> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getIdentity().equals(identity))
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(identity, token)) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            Assert.assertTrue(expectedTotal > 0);

            assertListResponse(r,
                    expectedResultSize,
                    expectedOffset,
                    expectedLimit,
                    expectedTotal);
        }
    }

    /**
     * Test that an invalid identity throws an error.
     */
    @Test
    public void testSearchByInvalidIdentity() {
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("identity", IdUtil.toString(IdUtil.next()));

        Response r = search(params, token);
        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Test that an malformed identity throws an error.
     */
    // TODO(krotscheck): This should return a 400.
    @Test
    public void testSearchByMalformedIdentity() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("identity", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Test that we can filter a search by an client ID.
     */
    @Test
    public void testSearchByClient() {
        String query = "many";
        Client client = getSecondaryContext()
                .getClient();

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("client", IdUtil.toString(client.getId()));
        Response r = search(params, token);

        // Determine result set.
        List<OAuthToken> searchResults = getSearchResults(query);
        List<OAuthToken> accessibleEntities = getAccessibleEntities(token);

        List<OAuthToken> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getClient().equals(client))
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(client, token)) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            Assert.assertTrue(expectedTotal > 0);

            assertListResponse(r,
                    expectedResultSize,
                    expectedOffset,
                    expectedLimit,
                    expectedTotal);
        }
    }

    /**
     * Test that an invalid client throws an error.
     */
    @Test
    public void testSearchByInvalidClient() {
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("client", IdUtil.toString(IdUtil.next()));

        Response r = search(params, token);
        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Test that an malformed client throws an error.
     */
    // TODO(krotscheck): This should return a 400.
    @Test
    public void testSearchByMalformedClient() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("client", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Test that we can filter a search by an client ID.
     */
    @Test
    public void testSearchByType() {
        String query = "many";
        OAuthTokenType type = OAuthTokenType.Bearer;

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("type", type.toString());
        Response r = search(params, token);

        // Determine result set.
        List<OAuthToken> searchResults = getSearchResults(query);
        List<OAuthToken> accessibleEntities = getAccessibleEntities(token);

        List<OAuthToken> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getTokenType().equals(type))
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            Assert.assertTrue(expectedTotal > 0);

            assertListResponse(r,
                    expectedResultSize,
                    expectedOffset,
                    expectedLimit,
                    expectedTotal);
        }
    }

    /**
     * Test that an invalid client throws an error.
     */
    @Test
    public void testSearchByInvalidType() {
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("type", "Invalid Type");

        Response r = search(params, token);
        assertErrorResponse(r, Status.NOT_FOUND);
    }
}
