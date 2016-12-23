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

import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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
    protected String getUrlForId(final String id) {
        if (id == null) {
            return "/authenticator/";
        }
        return String.format("/authenticator/%s", id);
    }

    /**
     * Extract the appropriate entity from a provided context.
     *
     * @return The client currently active in the admin app.
     */
    @Override
    protected Authenticator getEntity(final EnvironmentBuilder context) {
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
    protected Authenticator createValidEntity(final EnvironmentBuilder context) {
        Authenticator a = new Authenticator();
        a.setClient(context.getClient());
        a.setType("password");
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
     * Assert that the type MUST be one that is registered with the system.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostUnregisteredType() throws Exception {
        Authenticator testEntity = createValidEntity(getAdminContext());
        testEntity.setType("not_a_registered_string");

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
     * Assert that an empty type errors.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostEmptyType() throws Exception {
        Authenticator testEntity = createValidEntity(getAdminContext());
        testEntity.setType("");

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
        Authenticator a = getEntity(getSecondaryContext());
        a.getConfiguration().put("lol", "cat");
        a.getConfiguration().put("zing", "zong");

        Response r = putEntity(a, getAdminToken());

        if (shouldSucceed()) {
            Authenticator response = r.readEntity(Authenticator.class);
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
        Client newParent = getAdminContext().getClient();
        Authenticator entity = getEntity(getSecondaryContext());

        Authenticator authenticator = new Authenticator();
        authenticator.setId(entity.getId());
        authenticator.setType(entity.getType());
        authenticator.setClient(newParent);

        // Issue the request.
        Response r = putEntity(authenticator, getAdminToken());
        if (shouldSucceed()) {
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
    public void testPutInvalidateType() throws Exception {
        Authenticator entity = getEntity(getSecondaryContext());
        entity.setType("invalid_type");

        // Issue the request.
        Response r = putEntity(entity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
