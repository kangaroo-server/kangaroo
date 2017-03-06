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

import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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
public final class ScopeServiceBrowseTest
        extends DAbstractServiceBrowseTest<ApplicationScope> {

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public ScopeServiceBrowseTest(final ClientType clientType,
                                  final String tokenScope,
                                  final Boolean createUser) {
        super(clientType, tokenScope, createUser);
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.SCOPE_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.SCOPE;
    }

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<List<ApplicationScope>> getListType() {
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
    protected List<ApplicationScope> getAccessibleEntities(
            final OAuthToken token) {
        // If you're an admin, you get to see everything. If you're not, you
        // only get to see what you own.
        if (!token.getScopes().containsKey(getAdminScope())) {
            return getOwnedEntities(token);
        }

        // We know you're an admin. Get all applications in the system.
        Criteria c = getSession().createCriteria(Application.class);

        // Get all the owned clients.
        return ((List<Application>) c.list())
                .stream()
                .flatMap(a -> a.getScopes().values().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param owner The owner of these entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<ApplicationScope> getOwnedEntities(final User owner) {
        // Get all the owned clients.
        return owner.getApplications()
                .stream()
                .flatMap(a -> a.getScopes().values().stream())
                .collect(Collectors.toList());
    }

    /**
     * Test parameters.
     *
     * @return Test parameters.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.SCOPE_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.SCOPE,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.SCOPE_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.SCOPE,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.SCOPE_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.SCOPE,
                        false
                });
    }

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<List<ApplicationScope>> LIST_TYPE =
            new GenericType<List<ApplicationScope>>() {

            };

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected String getUrlForId(final String id) {
        if (StringUtils.isEmpty(id)) {
            return "/scope/";
        }
        return String.format("/scope/%s", id);
    }

    /**
     * Test that you can browse/filter by application.
     */
    @Test
    public void testBrowseByApplication() {
        OAuthToken token = getAdminToken();
        Application adminApp = getAdminContext().getApplication();
        Map<String, String> params = new HashMap<>();
        params.put("application", adminApp.getId().toString());

        Response r = browse(params, token);

        // Build expected results.
        List<ApplicationScope> expectedResults =
                getAccessibleEntities(token)
                        .stream()
                        .filter((scope) -> adminApp.equals(scope.getApplication()))
                        .distinct()
                        .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (isAccessible(adminApp, getAdminToken())) {
            List<ApplicationScope> results = r.readEntity(getListType());
            Assert.assertEquals(expectedOffset.toString(),
                    r.getHeaderString("Offset"));
            Assert.assertEquals(expectedLimit.toString(),
                    r.getHeaderString("Limit"));
            Assert.assertEquals(expectedTotal.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(expectedResultSize, results.size());
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Test that you can browse/filter by a role.
     */
    @Test
    public void testBrowseByRole() {
        OAuthToken token = getAdminToken();
        Role role = getAdminContext().getApplication()
                .getRoles()
                .stream()
                .filter((r) -> r.getName().equals("admin"))
                .collect(Collectors.toList())
                .get(0);
        Map<String, String> params = new HashMap<>();
        params.put("role", role.getId().toString());

        Response r = browse(params, token);

        // Build expected results.
        List<ApplicationScope> expectedResults =
                getAccessibleEntities(token)
                        .stream()
                        .filter((scope) -> scope.getRoles().contains(role))
                        .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (isAccessible(role, getAdminToken())) {
            List<ApplicationScope> results = r.readEntity(getListType());
            Assert.assertEquals(expectedOffset.toString(),
                    r.getHeaderString("Offset"));
            Assert.assertEquals(expectedLimit.toString(),
                    r.getHeaderString("Limit"));
            Assert.assertEquals(expectedTotal.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(expectedResultSize, results.size());
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }
}
