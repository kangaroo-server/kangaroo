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
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
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
 * Test the search endpoint of the authenticator service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class AuthenticatorServiceSearchTest
        extends AbstractServiceSearchTest<Authenticator> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<Authenticator>>
            LIST_TYPE = new GenericType<ListResponseEntity<Authenticator>>() {

    };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public AuthenticatorServiceSearchTest(final ClientType clientType,
                                          final String tokenScope,
                                          final Boolean createUser) {
        super(Authenticator.class, clientType, tokenScope, createUser);
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
                        Scope.AUTHENTICATOR_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.AUTHENTICATOR,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.AUTHENTICATOR_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.AUTHENTICATOR,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.AUTHENTICATOR_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.AUTHENTICATOR,
                        false
                });
    }

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<Authenticator>> getListType() {
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
    protected List<Authenticator> getOwnedEntities(final User owner) {
        // Get all the owned clients.
        return getAttached(owner).getApplications()
                .stream()
                .flatMap(a -> a.getClients().stream())
                .flatMap(c -> c.getAuthenticators().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Return the list of field names on which this particular entity type
     * has build a search index.
     *
     * @return An array of field names.
     */
    protected String[] getSearchIndexFields() {
        return new String[]{"client.name"};
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.AUTHENTICATOR_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.AUTHENTICATOR;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return UriBuilder.fromPath("/authenticator/")
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
     * Test that we can filter a search by an client ID.
     */
    @Test
    public void testSearchByClient() {
        String query = "many";
        // Find a client that we can filter by, which has an authenticator
        // that will match our search criteria.
        Client c = getSecondaryContext()
                .getApplication()
                .getClients()
                .stream()
                .flatMap(client -> client.getAuthenticators().stream())
                .filter(auth -> auth.getType().equals(AuthenticatorType.Test))
                .map(Authenticator::getClient)
                .collect(Collectors.toList())
                .get(0);

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("client", IdUtil.toString(c.getId()));
        Response r = search(params, token);

        // Determine result set.
        List<Authenticator> searchResults = getSearchResults(query);
        List<Authenticator> accessibleEntities = getAccessibleEntities(token);

        List<Authenticator> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getClient().equals(c))
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(c, token)) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {

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
    @Test
    public void testSearchByMalformedApplication() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("client", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
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
        List<Authenticator> searchResults = getSearchResults(query);
        List<Authenticator> accessibleEntities = getAccessibleEntities(token);

        List<Authenticator> expectedResults = searchResults
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
