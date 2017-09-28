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
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.hibernate.Criteria;
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
 * Test the list and filter methods of the scope service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class ClientServiceBrowseTest
        extends AbstractServiceBrowseTest<Client> {

    /**
     * Generic type declaration for list decoding.
     */
    private static final GenericType<ListResponseEntity<Client>> LIST_TYPE =
            new GenericType<ListResponseEntity<Client>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public ClientServiceBrowseTest(final ClientType clientType,
                                   final String tokenScope,
                                   final Boolean createUser) {
        super(clientType, tokenScope, createUser);
    }

    /**
     * Test parameters.
     *
     * @return The list of parameters.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.CLIENT_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.CLIENT,
                        false
                });
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.CLIENT_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.CLIENT;
    }

    /**
     * Return the list type used to decode browse results.
     *
     * @return The list type.
     */
    @Override
    protected GenericType<ListResponseEntity<Client>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the list of entities which should be accessible given a
     * specific token.
     *
     * @param token The oauth token to test against.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<Client> getAccessibleEntities(final OAuthToken token) {
        // If you're an admin, you get to see everything. If you're not, you
        // only get to see what you own.
        OAuthToken attachedToken = getAttached(token);
        if (!attachedToken.getScopes().containsKey(getAdminScope())) {
            return getOwnedEntities(attachedToken);
        }

        // We know you're an admin. Get all applications in the system.
        Criteria c = getSession().createCriteria(Application.class);

        // Get all the owned clients.
        return ((List<Application>) c.list())
                .stream()
                .flatMap(a -> a.getClients().stream())
                .collect(Collectors.toList());
    }

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param owner The owner of these entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<Client> getOwnedEntities(final User owner) {

        // Get all the owned clients.
        return owner.getApplications()
                .stream()
                .flatMap(a -> a.getClients().stream())
                .collect(Collectors.toList());
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return UriBuilder.fromPath("/client/")
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
        return getUrlForId(entity.getId().toString());
    }

    /**
     * Ensure that we can filter by the client's application.
     */
    @Test
    public void testBrowseFilterByApplication() {
        // Sometimes we own this application, sometimes we don't.
        Application filtered = getAdminContext().getApplication();

        Map<String, String> params = new HashMap<>();
        params.put("application", filtered.getId().toString());
        Response r = browse(params, getAdminToken());

        Long expectedEntities = getAccessibleEntities(getAdminToken())
                .stream()
                .filter(e -> e.getApplication().equals(filtered))
                .count();

        int expectedTotal = expectedEntities.intValue();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(filtered, getAdminToken())) {
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
     * Assert that we can filter by client type.
     */
    @Test
    public void testBrowseFilterByClientType() {
        Map<String, String> params = new HashMap<>();
        params.put("type", ClientType.AuthorizationGrant.toString());
        Response r = browse(params, getAdminToken());

        Long expectedEntities = getAccessibleEntities(getAdminToken())
                .stream()
                .filter(e -> e.getType().equals(ClientType.AuthorizationGrant))
                .count();

        int expectedTotal = expectedEntities.intValue();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            // Make sure we're actually testing against something here.
            Assert.assertTrue(expectedEntities > 0);

            assertListResponse(r,
                    expectedResultSize,
                    expectedOffset,
                    expectedLimit,
                    expectedTotal);
        }
    }
}
