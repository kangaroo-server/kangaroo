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

import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test the list and filter methods of the user identity service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class UserIdentityServiceBrowseTest
        extends AbstractServiceBrowseTest<UserIdentity> {

    /**
     * Generic type declaration for list decoding.
     */
    private static final GenericType<List<UserIdentity>> LIST_TYPE =
            new GenericType<List<UserIdentity>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public UserIdentityServiceBrowseTest(final ClientType clientType,
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
     * Return the list type used to decode browse results.
     *
     * @return The list type.
     */
    @Override
    protected GenericType<List<UserIdentity>> getListType() {
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
    protected List<UserIdentity> getAccessibleEntities(final OAuthToken token) {
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
                .flatMap(a -> a.getUsers().stream())
                .flatMap(u -> u.getIdentities().stream())
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
    protected List<UserIdentity> getOwnedEntities(final User owner) {

        // Get all the owned clients.
        return owner.getApplications()
                .stream()
                .flatMap(a -> a.getUsers().stream())
                .flatMap(u -> u.getIdentities().stream())
                .distinct()
                .collect(Collectors.toList());
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
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected String getUrlForId(final String id) {
        if (StringUtils.isEmpty(id)) {
            return "/identity/";
        }
        return String.format("/identity/%s", id);
    }

    /**
     * Ensure that we can filter by the identity's user.
     */
    @Test
    public void testBrowseFilterByUser() {
        User filtered = getAdminContext().getUserIdentity().getUser();

        Map<String, String> params = new HashMap<>();
        params.put("user", filtered.getId().toString());
        Response r = browse(params, getAdminToken());

        List<UserIdentity> expectedResults =
                getAccessibleEntities(getAdminToken())
                        .stream()
                        .filter((identity) ->
                                identity.getUser().equals(filtered))
                        .distinct()
                        .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (isAccessible(filtered, getAdminToken())) {
            List<UserIdentity> results = r.readEntity(getListType());
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
     * Ensure that we cannot filter by an invalid user.
     */
    @Test
    public void testBrowseFilterByInvalidUser() {
        Map<String, String> params = new HashMap<>();
        params.put("user", UUID.randomUUID().toString());
        Response r = browse(params, getAdminToken());

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Ensure that we cannot filter by a malformed user.
     */
    @Test
    public void testBrowseFilterByMalformedUser() {
        Map<String, String> params = new HashMap<>();
        params.put("user", "malformed");
        Response r = browse(params, getAdminToken());

        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Ensure that we can filter by the identity's Authenticator.
     */
    @Test
    public void testBrowseFilterByAuthenticator() {
        Authenticator filtered = getAdminContext().getAuthenticator();

        Map<String, String> params = new HashMap<>();
        params.put("authenticator", filtered.getId().toString());
        Response r = browse(params, getAdminToken());

        List<UserIdentity> expectedResults =
                getAccessibleEntities(getAdminToken())
                        .stream()
                        .filter((identity) ->
                                identity.getAuthenticator().equals(filtered))
                        .distinct()
                        .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (isAccessible(filtered, getAdminToken())) {
            List<UserIdentity> results = r.readEntity(getListType());
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
     * Ensure that we cannot filter by an invalid Authenticator.
     */
    @Test
    public void testBrowseFilterByInvalidAuthenticator() {
        Map<String, String> params = new HashMap<>();
        params.put("authenticator", UUID.randomUUID().toString());
        Response r = browse(params, getAdminToken());

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Ensure that we cannot filter by a malformed Authenticator.
     */
    @Test
    public void testBrowseFilterByMalformedAuthenticator() {
        Map<String, String> params = new HashMap<>();
        params.put("authenticator", "malformed");
        Response r = browse(params, getAdminToken());

        assertErrorResponse(r, Status.NOT_FOUND);
    }
}
