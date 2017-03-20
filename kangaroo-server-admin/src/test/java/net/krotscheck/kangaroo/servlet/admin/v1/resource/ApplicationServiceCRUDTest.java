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
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.ApplicationBuilder.ApplicationContext;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.hibernate.Session;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Unit tests for the /application endpoint's CRUD methods.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class ApplicationServiceCRUDTest
        extends AbstractServiceCRUDTest<Application> {

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public ApplicationServiceCRUDTest(final ClientType clientType,
                                      final String tokenScope,
                                      final Boolean createUser,
                                      final Boolean shouldSucceed) {
        super(Application.class, clientType, tokenScope, createUser,
                shouldSucceed);
    }

    /**
     * Return the correct testingEntity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    @Override
    protected Application getEntity(final ApplicationContext context) {
        return context.getApplication();
    }

    /**
     * Return a new, empty entity.
     *
     * @return The requested entity type under test.
     */
    @Override
    protected Application getNewEntity() {
        return new Application();
    }

    /**
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected Application createValidEntity(final ApplicationContext context) {
        Application a = new Application();
        a.setName(UUID.randomUUID().toString());
        a.setOwner(context.getOwner());
        return a;
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
        UriBuilder builder = UriBuilder.fromPath("/application/");
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
    protected URI getUrlForEntity(final AbstractEntity entity) {
        if (entity == null || entity.getId() == null) {
            return getUrlForId((String) null);
        }
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
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION,
                        true,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.APPLICATION_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.APPLICATION,
                        false,
                        false
                });
    }

    /**
     * Assert that an app cannot be created which overwrites another app.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostOverwrite() throws Exception {
        ApplicationContext context = getAdminContext();

        Application newApp = new Application();
        newApp.setId(getSecondaryContext().getApplication().getId());
        newApp.setName("New Application");
        newApp.setOwner(context.getUser());

        // Issue the request.
        Response r = postEntity(newApp, getAdminToken());

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test a really long name.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooLongName() throws Exception {
        ApplicationContext context = getAdminContext();

        Application newApp = new Application();
        newApp.setName(RandomStringUtils.randomAlphanumeric(257));
        newApp.setOwner(context.getUser());

        // Issue the request.
        Response r = postEntity(newApp, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test that an app created with no owner defaults to the current user.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoOwner() throws Exception {
        OAuthToken token = getAdminToken();

        Application newApp = new Application();
        newApp.setName("New Application");

        // Issue the request.
        Response r = postEntity(newApp, token);

        if (!token.getClient().getType().equals(ClientType.ClientCredentials)) {
            Assert.assertEquals(HttpStatus.SC_CREATED, r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse =
                    getEntity(r.getLocation(), getAdminToken());
            Application response =
                    getResponse.readEntity(Application.class);
            Assert.assertNotNull(response.getId());
            Assert.assertEquals(newApp.getName(), response.getName());
            Assert.assertNotNull(response.getOwner().getId());
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Test that an app can be created for another user.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostOwnerAssign() throws Exception {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .user()
                .identity()
                .build();
        OAuthToken token = getAdminToken();

        Application newApp = new Application();
        newApp.setName("New Application");
        newApp.setOwner(testContext.getUser());

        // Issue the request.
        Response r = postEntity(newApp, token);

        if (getTokenScope().equals(Scope.APPLICATION_ADMIN)) {
            Assert.assertEquals(HttpStatus.SC_CREATED, r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), getAdminToken());
            Application response = getResponse.readEntity(Application.class);

            Assert.assertNotNull(response.getId());
            Assert.assertEquals(newApp.getName(), response.getName());
            Assert.assertEquals(newApp.getOwner(), response.getOwner());
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that the admin app cannot be updated.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutAdminApp() throws Exception {
        Application app = getAdminContext().getApplication();
        app.setName(UUID.randomUUID().toString());

        Response r = putEntity(app, getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a regular app can be updated, from the admin app, with
     * appropriate credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRegularApp() throws Exception {
        Application a = getSecondaryContext().getApplication();
        a.setName(UUID.randomUUID().toString());
        Response r = putEntity(a, getAdminToken());

        if (isAccessible(a, getAdminToken())) {
            Application response = r.readEntity(Application.class);
            Assert.assertEquals(HttpStatus.SC_OK, r.getStatus());
            Assert.assertEquals(a, response);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a regular app cannot have its owner changed.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeOwner() throws Exception {

        // Create a new application for the secondary user.
        User oldOwner = getAdminContext().getOwner();
        Application newApp = createValidEntity(getAdminContext());
        newApp.setOwner(oldOwner);
        Session s = getSession();
        s.getTransaction().begin();
        s.save(newApp);
        s.getTransaction().commit();

        // Try to change the ownership to the admin user.
        User newOwner = getSecondaryContext().getOwner();
        newApp.setOwner(newOwner);

        // Issue the request.
        Response r = putEntity(newApp, getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that the admin app cannot be deleted, even if we have all the
     * credentials in the world.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testDeleteAdminApp() throws Exception {
        ApplicationContext context = getAdminContext();

        // Issue the request.
        Response r = deleteEntity(context.getApplication(), getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Sanity test for coverage on the scope getters.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testScopes() throws Exception {
        ApplicationService as = new ApplicationService();

        Assert.assertEquals(Scope.APPLICATION_ADMIN, as.getAdminScope());
        Assert.assertEquals(Scope.APPLICATION, as.getAccessScope());
    }
}
