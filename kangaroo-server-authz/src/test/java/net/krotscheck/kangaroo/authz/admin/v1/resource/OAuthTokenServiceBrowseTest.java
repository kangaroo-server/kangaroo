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
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidScopeException;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.hibernate.Criteria;
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
 * Test the list and filter methods of the token service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class OAuthTokenServiceBrowseTest
        extends AbstractServiceBrowseTest<OAuthToken> {

    /**
     * Generic type declaration for list decoding.
     */
    private static final GenericType<ListResponseEntity<OAuthToken>> LIST_TYPE =
            new GenericType<ListResponseEntity<OAuthToken>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public OAuthTokenServiceBrowseTest(final ClientType clientType,
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
     * Return the list type used to decode browse results.
     *
     * @return The list type.
     */
    @Override
    protected GenericType<ListResponseEntity<OAuthToken>> getListType() {
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
    protected List<OAuthToken> getAccessibleEntities(final OAuthToken token) {
        // If you're an admin, you get to see everything. If you're not, you
        // only get to see what you own.
        OAuthToken attachedToken = getAttached(token);
        if (!attachedToken.getScopes().containsKey(getAdminScope())) {
            return getOwnedEntities(attachedToken);
        }

        // We know you're an admin. Get all applications in the system.
        Criteria criteria = getSession().createCriteria(OAuthToken.class);

        // Get all the owned clients.
        return ((List<OAuthToken>) criteria.list());
    }

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param owner The owner of these entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<OAuthToken> getOwnedEntities(final User owner) {

        // Get all the owned clients.
        return owner.getApplications()
                .stream()
                .flatMap(a -> a.getClients().stream())
                .flatMap(c -> c.getTokens().stream())
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
     * Ensure that we can browse by client.
     */
    @Test
    public void testBrowseFilterByClient() {
        Client c = getAdminContext().getClient();
        Map<String, String> params = new HashMap<>();
        params.put("client", IdUtil.toString(c.getId()));
        Response r = browse(params, getAdminToken());

        List<OAuthToken> expectedResults =
                getAccessibleEntities(getAdminToken())
                        .stream()
                        .filter((token) -> token.getClient().equals(c))
                        .distinct()
                        .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, new InvalidScopeException());
        } else if (isAccessible(c, getAdminToken())) {
            assertListResponse(r,
                    expectedResultSize,
                    expectedOffset,
                    expectedLimit,
                    expectedTotal);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Ensure that we can browse by identity.
     */
    @Test
    public void testBrowseFilterByIdentity() {
        UserIdentity identity = getAdminContext().getUserIdentity();
        Map<String, String> params = new HashMap<>();
        params.put("identity", IdUtil.toString(identity.getId()));
        Response r = browse(params, getAdminToken());

        List<OAuthToken> expectedResults =
                getAccessibleEntities(getAdminToken())
                        .stream()
                        .filter((token) -> token.getIdentity() != null)
                        .filter((token) -> token.getIdentity().equals(identity))
                        .distinct()
                        .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, new InvalidScopeException());
        } else if (isAccessible(identity, getAdminToken())) {
            assertListResponse(r,
                    expectedResultSize,
                    expectedOffset,
                    expectedLimit,
                    expectedTotal);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }
}
