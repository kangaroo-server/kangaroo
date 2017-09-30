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
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.junit.Assert;
import org.junit.Test;
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
 * Tests for the User Identity search API.
 *
 * @author Michael Krotscheck
 */
public final class UserIdentityServiceSearchTest
        extends AbstractServiceSearchTest<UserIdentity> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<UserIdentity>>
            LIST_TYPE = new GenericType<ListResponseEntity<UserIdentity>>() {

    };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public UserIdentityServiceSearchTest(final ClientType clientType,
                                         final String tokenScope,
                                         final Boolean createUser) {
        super(UserIdentity.class, clientType, tokenScope, createUser);
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
                        Scope.IDENTITY_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.IDENTITY,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.IDENTITY_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.IDENTITY,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.IDENTITY_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.IDENTITY,
                        false
                });
    }


    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<UserIdentity>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param owner The owner of these entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<UserIdentity> getOwnedEntities(final User owner) {
        // Get all the owned clients.
        return getAttached(owner).getApplications()
                .stream()
                .flatMap(a -> a.getUsers().stream())
                .flatMap(u -> u.getIdentities().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Return the list of field names on which this particular entity type
     * has build a search index.
     *
     * @return An array of field names.
     */
    @Override
    protected String[] getSearchIndexFields() {
        return new String[]{"claims", "remoteId"};
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.IDENTITY_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.IDENTITY;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return UriBuilder.fromPath("/identity/")
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
     * Test that we can filter a search by a user ID.
     */
    @Test
    public void testSearchByUser() {
        // Find a user with some appropriate search results
        String query = "many";
        List<UserIdentity> searchResults = getSearchResults(query);
        User user = searchResults.stream()
                .map(i -> i.getUser())
                .distinct()
                .collect(Collectors.toList())
                .get(0);

        // Build the query.
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("user", IdUtil.toString(user.getId()));
        Response r = search(params, token);

        // Determine expected result set.
        List<UserIdentity> accessibleEntities = getAccessibleEntities(token);
        List<UserIdentity> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getUser().equals(user))
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
     * Test that an invalid application throws an error.
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
     * Test that an malformed application throws an error.
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
     * Test that we can filter a search by an identity type.
     */
    @Test
    public void testSearchByType() {
        String query = "many";
        Authenticator authenticator = getSecondaryContext().getAuthenticator();
        AuthenticatorType type = authenticator.getType();

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("type", type.toString());
        Response r = search(params, token);

        // Determine result set.
        List<UserIdentity> searchResults = getSearchResults(query);
        List<UserIdentity> accessibleEntities = getAccessibleEntities(token);

        List<UserIdentity> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getType().equals(type))
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
     * Test that an malformed application throws an error.
     */
    // TODO(krotscheck): This should return a 400.
    @Test
    public void testSearchByInvalidType() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("type", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }
}
