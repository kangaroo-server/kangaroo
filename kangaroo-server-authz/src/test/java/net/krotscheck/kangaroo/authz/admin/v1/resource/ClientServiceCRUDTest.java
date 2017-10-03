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
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.apache.commons.lang.RandomStringUtils;
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
 * Test the CRUD methods of the scope service.
 *
 * @author Michael Krotscheck
 */
public final class ClientServiceCRUDTest
        extends AbstractServiceCRUDTest<Client> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<Client>> LIST_TYPE =
            new GenericType<ListResponseEntity<Client>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public ClientServiceCRUDTest(final ClientType clientType,
                                 final String tokenScope,
                                 final Boolean createUser,
                                 final Boolean shouldSucceed) {
        super(Client.class, clientType, tokenScope, createUser, shouldSucceed);
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
                        Scope.CLIENT_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT,
                        true,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.CLIENT_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.CLIENT,
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
    protected GenericType<ListResponseEntity<Client>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        UriBuilder builder = UriBuilder.fromPath("/client/");
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
     * Extract the appropriate entity from a provided context.
     *
     * @return The client currently active in the admin app.
     */
    @Override
    protected Client getEntity(final ApplicationContext context) {
        return context.getClient();
    }

    /**
     * Create a brand new entity.
     *
     * @return A brand new entity!
     */
    @Override
    protected Client getNewEntity() {
        return new Client();
    }


    /**
     * Return the token scope required for admin access.
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
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected Client createValidEntity(final ApplicationContext context) {
        Client c = new Client();
        c.setApplication(context.getApplication());
        c.setName(RandomStringUtils.randomAlphanumeric(10));
        c.setClientSecret(RandomStringUtils.randomAlphanumeric(10));
        c.setType(ClientType.ClientCredentials);

        c.getConfiguration().put("foo", "bar");
        c.getConfiguration().put("lol", "cat");
        return c;
    }

    /**
     * Assert that you cannot create a client without an application
     * reference.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoParent() throws Exception {
        Client testEntity = createValidEntity(getAdminContext());
        testEntity.setApplication(null);

        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test a really long name.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooLongName() throws Exception {
        Client testEntity = createValidEntity(getAdminContext());
        testEntity.setName(RandomStringUtils.randomAlphanumeric(257));

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test that the client type is required.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoType() throws Exception {
        Client testEntity = createValidEntity(getAdminContext());
        testEntity.setType(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test that the client secret is not required.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoSecret() throws Exception {
        Client testEntity = createValidEntity(getAdminContext());
        testEntity.setClientSecret(null);

        Response r = postEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), getAdminToken());
            Client response = getResponse.readEntity(Client.class);
            assertContentEquals(testEntity, response);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that we cannot modify the client we're currently using to
     * access this application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutCannotModifyCurrentClient() throws Exception {
        Client client = getAdminToken().getClient();
        Client newClient = new Client();
        newClient.setId(client.getId());
        newClient.setName("New Name");
        newClient.setApplication(client.getApplication());
        newClient.setClientSecret(client.getClientSecret());
        newClient.setType(client.getType());

        Response r = putEntity(newClient, getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.CONFLICT);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a regular entity can be updated, from the admin app, with
     * appropriate credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRegularEntity() throws Exception {
        Client a = getEntity(getSecondaryContext());
        a.setName(IdUtil.toString(IdUtil.next()));
        a.setType(ClientType.OwnerCredentials);
        a.setClientSecret(IdUtil.toString(IdUtil.next()));
        Response r = putEntity(a, getAdminToken());

        if (isAccessible(a, getAdminToken())) {
            Client response = r.readEntity(Client.class);
            Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
            Assert.assertEquals(a, response);
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
        Client entity = getEntity(getSecondaryContext());

        Client client = new Client();
        client.setId(entity.getId());
        client.setName(entity.getName());
        client.setType(entity.getType());
        client.setApplication(newParent);

        // Issue the request.
        Response r = putEntity(client, getAdminToken());
        if (isAccessible(entity, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot delete the client we're currently using to
     * access this application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testCannotDeleteCurrentClient() throws Exception {
        Client client = getAdminToken().getClient();

        Response r = deleteEntity(client, getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.CONFLICT);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
