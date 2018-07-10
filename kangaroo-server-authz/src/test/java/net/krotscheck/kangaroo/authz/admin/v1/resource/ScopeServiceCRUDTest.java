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
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Session;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Test the CRUD methods of the scope service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class ScopeServiceCRUDTest
        extends AbstractServiceCRUDTest<ApplicationScope> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<ApplicationScope>>
            LIST_TYPE =
            new GenericType<ListResponseEntity<ApplicationScope>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public ScopeServiceCRUDTest(final ClientType clientType,
                                final String tokenScope,
                                final Boolean createUser,
                                final Boolean shouldSucceed) {
        super(ApplicationScope.class, clientType, tokenScope, createUser,
                shouldSucceed);
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
                        Scope.SCOPE_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.SCOPE,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.SCOPE_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.SCOPE,
                        true,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.SCOPE_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.SCOPE,
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
    protected GenericType<ListResponseEntity<ApplicationScope>> getListType() {
        return LIST_TYPE;
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
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        UriBuilder builder = UriBuilder.fromPath("/v1/scope/");
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
     * Return the correct testingEntity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    @Override
    protected ApplicationScope getEntity(final ApplicationContext context) {
        return context.getScope();
    }

    /**
     * Return a new, empty entity.
     *
     * @return The requested entity type under test.
     */
    @Override
    protected ApplicationScope getNewEntity() {
        return new ApplicationScope();
    }

    /**
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected ApplicationScope createValidEntity(
            final ApplicationContext context) {
        ApplicationScope s = new ApplicationScope();
        s.setName(RandomStringUtils.randomAlphabetic(10));
        s.setApplication(context.getApplication());
        return s;
    }

    /**
     * Assert that you cannot create a duplicate scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostDuplicateScopeName() throws Exception {
        OAuthToken token = getAdminToken();

        // Extract an application the user has access to.
        Application parentApp = token.getClient().getApplication();
        // Grab the first scope name.
        String scopeName = parentApp.getScopes().values().iterator().next()
                .getName();

        // Build a new scope for that app.
        ApplicationScope newScope = new ApplicationScope();
        newScope.setApplication(parentApp);
        newScope.setName(scopeName);

        // Issue the request.
        Response r = postEntity(newScope, token);

        if (shouldSucceed()) {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.CONFLICT.getStatusCode(),
                    r.getStatus());
            assertEquals("conflict", response.getError());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.BAD_REQUEST.getStatusCode(),
                    r.getStatus());
            assertEquals("bad_request", response.getError());
        }
    }

    /**
     * Assert that you cannot post an entity without an application reference.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoParent() throws Exception {
        OAuthToken token = getAdminToken();
        ApplicationScope newScope = new ApplicationScope();
        newScope.setName(RandomStringUtils.random(20));

        // Issue the request.
        Response r = postEntity(newScope, token);

        ErrorResponse response = r.readEntity(ErrorResponse.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        assertEquals("bad_request", response.getError());
    }

    /**
     * Assert that a scope cannot be created which overwrites another scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostOverwrite() throws Exception {
        ApplicationContext otherApp = getSecondaryContext();
        ApplicationScope newScope = new ApplicationScope();
        newScope.setId(otherApp.getScope().getId());
        newScope.setName(RandomStringUtils.random(20));
        newScope.setApplication(otherApp.getScope().getApplication());

        // Issue the request.
        Response r = postEntity(newScope, getAdminToken());

        ErrorResponse response = r.readEntity(ErrorResponse.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        assertEquals("bad_request", response.getError());
    }

    /**
     * Test a really long name.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooLongName() throws Exception {
        ApplicationScope newScope = createValidEntity(getAdminContext());
        newScope.setName(RandomStringUtils.randomAlphanumeric(257));

        // Issue the request.
        Response r = postEntity(newScope, getAdminToken());

        ErrorResponse response = r.readEntity(ErrorResponse.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        assertEquals("bad_request", response.getError());
    }

    /**
     * Test that a scope can be created for another application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostApplicationAssign() throws Exception {
        OAuthToken token = getAdminToken();
        ApplicationContext yetAnotherApp = ApplicationBuilder
                .newApplication(getSession())
                .build();

        ApplicationScope newScope = new ApplicationScope();
        newScope.setName(RandomStringUtils.randomAlphanumeric(20));
        newScope.setApplication(yetAnotherApp.getApplication());

        // Issue the request.
        Response r = postEntity(newScope, token);

        if (getTokenScope().equals(getAdminScope())) {
            assertEquals(Status.CREATED.getStatusCode(),
                    r.getStatus());
            assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), token);
            ApplicationScope response =
                    getResponse.readEntity(ApplicationScope.class);
            assertNotNull(response.getId());
            assertEquals(newScope.getName(), response.getName());
            assertEquals(yetAnotherApp.getApplication(),
                    response.getApplication());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.BAD_REQUEST.getStatusCode(),
                    r.getStatus());
            assertEquals("bad_request", response.getError());
        }
    }

    /**
     * Assert that an admin scope cannot be updated.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutAdminScope() throws Exception {
        String newName = IdUtil.toString(IdUtil.next());
        ApplicationScope scope = getAdminContext().getScope();
        scope.setName(newName);

        Response r = putEntity(scope, getAdminToken());

        if (shouldSucceed()) {
            assertEquals(Status.FORBIDDEN.getStatusCode(),
                    r.getStatus());
            ErrorResponse result = r.readEntity(ErrorResponse.class);
            assertEquals("forbidden", result.getError());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.NOT_FOUND.getStatusCode(),
                    r.getStatus());
            assertEquals("not_found", response.getError());
        }
    }

    /**
     * Assert that a regular scope can be updated, from the admin app, with
     * appropriate credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRegularScope() throws Exception {
        // Create an entity to edit.
        ApplicationScope scope = createValidEntity(getSecondaryContext());
        Session s = getSession();
        s.getTransaction().begin();
        s.save(scope);
        s.getTransaction().commit();

        String newName = IdUtil.toString(IdUtil.next());
        scope.setName(newName);
        Response r = putEntity(scope, getAdminToken());

        if (isAccessible(scope, getAdminToken())) {
            ApplicationScope response = r.readEntity(ApplicationScope.class);
            assertEquals(Status.OK.getStatusCode(),
                    r.getStatus());
            assertEquals(newName, response.getName());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.NOT_FOUND.getStatusCode(),
                    r.getStatus());
            assertEquals("not_found", response.getError());
        }
    }

    /**
     * Assert that a regular scope cannot have its application changed.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeOwner() throws Exception {
        Application app = getAdminContext().getApplication();

        ApplicationScope scope = new ApplicationScope();
        scope.setId(getSecondaryContext().getScope().getId());
        scope.setName(getSecondaryContext().getScope().getName());
        scope.setApplication(app);

        // Issue the request.
        Response r = putEntity(scope, getAdminToken());

        if (isAccessible(getSecondaryContext().getScope(), getAdminToken())) {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.BAD_REQUEST.getStatusCode(),
                    r.getStatus());
            assertEquals("bad_request", response.getError());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.NOT_FOUND.getStatusCode(),
                    r.getStatus());
            assertEquals("not_found", response.getError());
        }
    }

    /**
     * Assert that the admin scope cannot be deleted, even if we have all the
     * credentials in the world.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testDeleteAdminScope() throws Exception {
        ApplicationContext context = getAdminContext();

        // Issue the request.
        Response r = deleteEntity(context.getScope(), getAdminToken());

        if (shouldSucceed()) {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.FORBIDDEN.getStatusCode(),
                    r.getStatus());
            assertEquals("forbidden", response.getError());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.NOT_FOUND.getStatusCode(),
                    r.getStatus());
            assertEquals("not_found", response.getError());
        }
    }

    /**
     * Assert that a regular app can be deleted, from the admin app, with
     * appropriate credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testDeleteRegularScope() throws Exception {
        // Create an entity to delete.
        ApplicationScope scope = createValidEntity(getSecondaryContext());
        Session s = getSession();
        s.getTransaction().begin();
        s.save(scope);
        s.getTransaction().commit();

        // Issue the request.
        Response r = deleteEntity(scope, getAdminToken());

        if (isAccessible(scope, getAdminToken())) {
            assertEquals(Status.RESET_CONTENT.getStatusCode(),
                    r.getStatus());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            assertEquals(Status.NOT_FOUND.getStatusCode(),
                    r.getStatus());
            assertEquals("not_found", response.getError());
        }
    }
}
