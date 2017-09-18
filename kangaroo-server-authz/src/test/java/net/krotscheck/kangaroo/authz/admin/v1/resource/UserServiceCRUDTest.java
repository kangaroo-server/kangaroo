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
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
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

/**
 * Tests for the UserService CRUD actions.
 *
 * @author Michael Krotscheck
 */
public final class UserServiceCRUDTest
        extends AbstractServiceCRUDTest<User> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<User>> LIST_TYPE =
            new GenericType<ListResponseEntity<User>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public UserServiceCRUDTest(final ClientType clientType,
                               final String tokenScope,
                               final Boolean createUser,
                               final Boolean shouldSucceed) {
        super(User.class, clientType, tokenScope, createUser,
                shouldSucceed);
    }

    /**
     * Test parameters.
     *
     * @return List of parameters used to reconstruct this test.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.USER_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.USER,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.USER_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.USER,
                        true,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.USER_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.USER,
                        false,
                        false
                });
    }

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<User>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the correct testingEntity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    @Override
    protected User getEntity(final ApplicationContext context) {
        return context.getUser();
    }

    /**
     * Return a new, empty entity.
     *
     * @return The requested entity type under test.
     */
    @Override
    protected User getNewEntity() {
        return new User();
    }

    /**
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected User createValidEntity(final ApplicationContext context) {
        User newUser = new User();
        newUser.setApplication(context.getApplication());
        newUser.setRole(context.getRole());
        return newUser;
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
        UriBuilder builder = UriBuilder.fromPath("/user/");
        if (id != null) {
            builder.path(id);
        }
        return builder.build();
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractAuthzEntity entity) {
        if (entity == null || entity.getId() == null) {
            return getUrlForId((String) null);
        }
        return getUrlForId(IdUtil.toString(entity.getId()));
    }

    /**
     * Assert that you cannot create a client without an application
     * reference.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoParent() throws Exception {
        User testEntity = createValidEntity(getAdminContext());
        testEntity.setApplication(null);

        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that we can modify a user.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPut() throws Exception {
        User entity = getEntity(getAdminContext());
        Role role = getAdminContext().getRole();
        entity.setRole(role);
        entity.setApplication(getAdminContext().getApplication());

        // Issue the request.
        Response r = putEntity(entity, getAdminToken());
        if (shouldSucceed()) {
            User response = r.readEntity(User.class);
            Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
            Assert.assertEquals(entity, response);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a regular entity cannot have its parent changed.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeParentEntity() throws Exception {
        Application newParent = getAdminContext().getApplication();
        User entity = getEntity(getSecondaryContext());

        User user = new User();
        user.setId(entity.getId());
        user.setApplication(newParent);
        user.setRole(entity.getRole());

        // Issue the request.
        Response r = putEntity(user, getAdminToken());
        if (isAccessible(entity, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
