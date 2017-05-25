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

import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.admin.Scope;
import org.hibernate.Criteria;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Unit tests for the /application endpoint's browse and /search methods.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class ApplicationServiceBrowseTest
        extends AbstractServiceBrowseTest<Application> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<List<Application>> LIST_TYPE =
            new GenericType<List<Application>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public ApplicationServiceBrowseTest(final ClientType clientType,
                                        final String tokenScope,
                                        final Boolean createUser) {
        super(clientType, tokenScope, createUser);
    }

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<List<Application>> getListType() {
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
    protected List<Application> getAccessibleEntities(final OAuthToken token) {
        // If you're an admin, you get to see everything. If you're not, you
        // only get to see what you own.
        OAuthToken attachedToken = getAttached(token);
        if (!attachedToken.getScopes().containsKey(getAdminScope())) {
            return getOwnedEntities(attachedToken);
        }

        // We know you're an admin. Get all applications in the system.
        Criteria c = getSession().createCriteria(Application.class);

        // Get all the owned clients.
        return ((List<Application>) c.list());
    }

    /**
     * Return the list of entities which are owned by the given user.
     *
     * @param owner The owner of the entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<Application> getOwnedEntities(final User owner) {
        return getAttached(owner).getApplications();
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.APPLICATION_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.APPLICATION;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return UriBuilder.fromPath("/application/")
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
     * Test parameters.
     *
     * @return A list of parameters used to initialize the test class.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.APPLICATION_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.APPLICATION,
                        false
                });
    }
}
