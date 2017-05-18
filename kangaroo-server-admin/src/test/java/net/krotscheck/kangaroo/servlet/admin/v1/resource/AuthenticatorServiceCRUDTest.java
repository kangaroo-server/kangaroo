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

import net.krotscheck.kangaroo.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.ApplicationBuilder.ApplicationContext;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Test the CRUD methods of the Authenticator service.
 *
 * @author Michael Krotscheck
 */
public final class AuthenticatorServiceCRUDTest
        extends AbstractServiceCRUDTest<Authenticator> {

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public AuthenticatorServiceCRUDTest(final ClientType clientType,
                                        final String tokenScope,
                                        final Boolean createUser,
                                        final Boolean shouldSucceed) {
        super(Authenticator.class, clientType, tokenScope, createUser,
                shouldSucceed);
    }

    /**
     * Test parameters.
     *
     * @return List of parameters used to reconstruct this test.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(new Object[][]{
                {
                        ClientType.Implicit,
                        Scope.AUTHENTICATOR_ADMIN,
                        false,
                        true
                },
                {
                        ClientType.Implicit,
                        Scope.AUTHENTICATOR,
                        false,
                        true
                },
                {
                        ClientType.Implicit,
                        Scope.AUTHENTICATOR_ADMIN,
                        true,
                        true
                },
                {
                        ClientType.Implicit,
                        Scope.AUTHENTICATOR,
                        true,
                        false
                },
                {
                        ClientType.ClientCredentials,
                        Scope.AUTHENTICATOR_ADMIN,
                        false,
                        true
                },
                {
                        ClientType.ClientCredentials,
                        Scope.AUTHENTICATOR,
                        false,
                        false
                }
        });
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        UriBuilder builder = UriBuilder.fromPath("/authenticator/");
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
     * Extract the appropriate entity from a provided context.
     *
     * @return The client currently active in the admin app.
     */
    @Override
    protected Authenticator getEntity(final ApplicationContext context) {
        return context.getAuthenticator();
    }

    /**
     * Create a brand new entity.
     *
     * @return A brand new entity!
     */
    @Override
    protected Authenticator getNewEntity() {
        return new Authenticator();
    }


    /**
     * Return the token scope required for admin access.
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
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected Authenticator createValidEntity(
            final ApplicationContext context) {
        Authenticator a = new Authenticator();
        a.setClient(context.getClient());
        a.setType(AuthenticatorType.Test);
        a.getConfiguration().put("foo", "bar");
        a.getConfiguration().put("lol", "cat");
        return a;
    }

    /**
     * Assert that you cannot create an authenticator without a client
     * reference.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoParent() throws Exception {
        Authenticator testEntity = createValidEntity(getAdminContext());
        testEntity.setClient(null);

        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that authenticators must be linked to a valid parent.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostInvalidParent() throws Exception {
        Authenticator testEntity = createValidEntity(getAdminContext());
        Client wrongParent = new Client();
        wrongParent.setId(UUID.randomUUID());
        testEntity.setClient(wrongParent);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that the type must be set.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoType() throws Exception {
        Authenticator testEntity = createValidEntity(getAdminContext());
        testEntity.setType(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that a regular entity can be updated, from the admin app, with
     * appropriate credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPut() throws Exception {
        // Create an entity to update
        Authenticator a = createValidEntity(getSecondaryContext());
        Session s = getSession();
        s.getTransaction().begin();
        s.save(a);
        s.getTransaction().commit();

        // Update the entity
        a.getConfiguration().put("foo", "baz");
        a.getConfiguration().put("zing", "zong");

        Response r = putEntity(a, getAdminToken());

        if (isAccessible(a, getAdminToken())) {
            Authenticator response = r.readEntity(Authenticator.class);
            Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
            Assert.assertEquals(a, response);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }

        // Cleanup the authenticator
        s.getTransaction().begin();
        s.delete(a);
        s.getTransaction().commit();
    }

    /**
     * Assert that a regular entity cannot have its parent changed.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeParentEntity() throws Exception {
        Client newParent = getAdminContext().getClient();
        Authenticator entity = getEntity(getSecondaryContext());

        Assert.assertNotEquals(newParent, entity.getClient());

        Authenticator authenticator = new Authenticator();
        authenticator.setId(entity.getId());
        authenticator.setType(entity.getType());
        authenticator.setClient(newParent);

        // Issue the request.
        Response r = putEntity(authenticator, getAdminToken());
        if (isAccessible(entity, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot update to an invalid authenticator type.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutNoType() throws Exception {
        Authenticator entity = getEntity(getSecondaryContext());
        entity.setType(null);

        // Issue the request.
        Response r = putEntity(entity, getAdminToken());
        if (isAccessible(entity, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
