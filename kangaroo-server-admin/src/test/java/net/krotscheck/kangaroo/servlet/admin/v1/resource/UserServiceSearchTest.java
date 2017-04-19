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

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tests for the UserService API.
 *
 * @author Michael Krotscheck
 */
public final class UserServiceSearchTest
        extends AbstractServiceSearchTest<User> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<List<User>> LIST_TYPE =
            new GenericType<List<User>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public UserServiceSearchTest(final ClientType clientType,
                                 final String tokenScope,
                                 final Boolean createUser) {
        super(User.class, clientType, tokenScope, createUser);
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
                        Scope.USER_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.USER,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.USER_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.USER,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.USER_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.USER,
                        false
                });
    }


    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<List<User>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param owner The owner of these entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<User> getOwnedEntities(final User owner) {
        // Get all the owned clients.
        return getAttached(owner).getApplications()
                .stream()
                .flatMap(a -> a.getUsers().stream())
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
        return new String[]{"identities.claims", "identities.remoteId"};
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.USER_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.USER;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return UriBuilder.fromPath("/user/")
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
    protected URI getUrlForEntity(final AbstractEntity entity) {
        return getUrlForId(entity.getId().toString());
    }

    /**
     * Test that we can filter a search by an application ID.
     */
    @Test
    public void testSearchByApplication() {
        String query = "many";
        Application a = getSecondaryContext()
                .getApplication();

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("application", a.getId().toString());
        Response r = search(params, token);

        // Determine result set.
        List<User> searchResults = getSearchResults(query);
        List<User> accessibleEntities = getAccessibleEntities(token);

        List<User> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getApplication().equals(a))
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(a, token)) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            Assert.assertTrue(expectedTotal > 0);

            List<User> results = r.readEntity(getListType());
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
     * Test that an invalid application throws an error.
     */
    @Test
    public void testSearchByInvalidApplication() {
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("application", UUID.randomUUID().toString());

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
    public void testSearchByMalformedApplication() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("application", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Test that we can filter a search by an role ID.
     */
    @Test
    public void testSearchByRole() {
        String query = "many";
        Role role = getSecondaryContext()
                .getApplication()
                .getUsers()
                .stream()
                .filter(user -> user.getRole() != null)
                .collect(Collectors.toList())
                .get(0)
                .getRole();

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("role", role.getId().toString());
        Response r = search(params, token);

        // Determine result set.
        List<User> searchResults = getSearchResults(query);
        List<User> accessibleEntities = getAccessibleEntities(token);

        List<User> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter(item -> item.getRole() != null)
                .filter((item) -> item.getRole().equals(role))
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(role, token)) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            Assert.assertTrue(expectedTotal > 0);

            List<User> results = r.readEntity(getListType());
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
     * Test that an invalid role throws an error.
     */
    @Test
    public void testSearchByInvalidRole() {
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("role", UUID.randomUUID().toString());

        Response r = search(params, token);
        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Test that an malformed role throws an error.
     */
    // TODO(krotscheck): This should return a 400.
    @Test
    public void testSearchByMalformedRole() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("role", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }
}
