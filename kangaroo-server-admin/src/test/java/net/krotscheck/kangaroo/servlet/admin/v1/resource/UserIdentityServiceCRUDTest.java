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
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.util.PasswordUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test the CRUD methods of the UserIdentity service.
 *
 * @author Michael Krotscheck
 */
public final class UserIdentityServiceCRUDTest
        extends AbstractServiceCRUDTest<UserIdentity> {

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public UserIdentityServiceCRUDTest(final ClientType clientType,
                                       final String tokenScope,
                                       final Boolean createUser,
                                       final Boolean shouldSucceed) {
        super(UserIdentity.class, clientType, tokenScope, createUser,
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
                        Scope.IDENTITY_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.IDENTITY,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.IDENTITY_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.IDENTITY,
                        true,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.IDENTITY_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.IDENTITY,
                        false,
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
        if (id == null) {
            return "/identity/";
        }
        return String.format("/identity/%s", id);
    }

    /**
     * Extract the appropriate entity from a provided context.
     *
     * @return The client currently active in the admin app.
     */
    @Override
    protected UserIdentity getEntity(final EnvironmentBuilder context) {
        return context.getUserIdentity();
    }

    /**
     * Create a brand new entity.
     *
     * @return A brand new entity!
     */
    @Override
    protected UserIdentity getNewEntity() {
        return new UserIdentity();
    }


    /**
     * Return the token scope required for admin access.
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
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected UserIdentity createValidEntity(final EnvironmentBuilder context) {
        UserIdentity identity = new UserIdentity();
        identity.setRemoteId(UUID.randomUUID().toString());
        identity.setPassword(UUID.randomUUID().toString());
        identity.setUser(context.getUser());
        identity.getClaims().put("foo", "bar");
        identity.getClaims().put("lol", "cat");

        // Find an appropriate authenticator
        Authenticator authenticator = context.getApplication().getClients()
                .stream()
                .flatMap((c) -> c.getAuthenticators().stream())
                .filter((a -> a.getType().equals("password")))
                .collect(Collectors.toList())
                .get(0);

        identity.setAuthenticator(authenticator);
        return identity;
    }
//
//    /**
//     * Assert that you cannot create an authenticator without a client
//     * reference.
//     *
//     * @throws Exception Exception encountered during test.
//     */
//    @Test
//    public void testPostNoParent() throws Exception {
//        Authenticator testEntity = createValidEntity(getAdminContext());
//        testEntity.setClient(null);
//
//        Response r = postEntity(testEntity, getAdminToken());
//        assertErrorResponse(r, Status.BAD_REQUEST);
//    }
//
//    /**
//     * Assert that authenticators must be linked to a valid parent.
//     *
//     * @throws Exception Exception encountered during test.
//     */
//    @Test
//    public void testPostInvalidParent() throws Exception {
//        Authenticator testEntity = createValidEntity(getAdminContext());
//        Client wrongParent = new Client();
//        wrongParent.setId(UUID.randomUUID());
//        testEntity.setClient(wrongParent);
//
//        // Issue the request.
//        Response r = postEntity(testEntity, getAdminToken());
//        assertErrorResponse(r, Status.BAD_REQUEST);
//    }
//
//    /**
//     * Assert that the type MUST be one that is registered with the system.
//     *
//     * @throws Exception Exception encountered during test.
//     */
//    @Test
//    public void testPostUnregisteredType() throws Exception {
//        Authenticator testEntity = createValidEntity(getAdminContext());
//        testEntity.setType("not_a_registered_string");
//
//        // Issue the request.
//        Response r = postEntity(testEntity, getAdminToken());
//        assertErrorResponse(r, Status.BAD_REQUEST);
//    }
//
//    /**
//     * Assert that the type must be set.
//     *
//     * @throws Exception Exception encountered during test.
//     */
//    @Test
//    public void testPostNoType() throws Exception {
//        Authenticator testEntity = createValidEntity(getAdminContext());
//        testEntity.setType(null);
//
//        // Issue the request.
//        Response r = postEntity(testEntity, getAdminToken());
//        assertErrorResponse(r, Status.BAD_REQUEST);
//    }
//
//    /**
//     * Assert that an empty type errors.
//     *
//     * @throws Exception Exception encountered during test.
//     */
//    @Test
//    public void testPostEmptyType() throws Exception {
//        Authenticator testEntity = createValidEntity(getAdminContext());
//        testEntity.setType("");
//
//        // Issue the request.
//        Response r = postEntity(testEntity, getAdminToken());
//        assertErrorResponse(r, Status.BAD_REQUEST);
//    }
//
//    /**
//     * Assert that a regular entity can be updated, from the admin app, with
//     * appropriate credentials.
//     *
//     * @throws Exception Exception encountered during test.
//     */
//    @Test
//    public void testPut() throws Exception {
//        Authenticator a = getEntity(getSecondaryContext());
//        a.getConfiguration().put("lol", "cat");
//        a.getConfiguration().put("zing", "zong");
//
//        Response r = putEntity(a, getAdminToken());
//
//        if (shouldSucceed()) {
//            Authenticator response = r.readEntity(Authenticator.class);
//            Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
//            Assert.assertEquals(a, response);
//        } else {
//            assertErrorResponse(r, Status.NOT_FOUND);
//        }
//    }
//
//    /**
//     * Assert that a regular entity cannot have its parent changed.
//     *
//     * @throws Exception Exception encountered during test.
//     */
//    @Test
//    public void testPutChangeParentEntity() throws Exception {
//        Client newParent = getAdminContext().getClient();
//        Authenticator entity = getEntity(getSecondaryContext());
//
//        Authenticator authenticator = new Authenticator();
//        authenticator.setId(entity.getId());
//        authenticator.setType(entity.getType());
//        authenticator.setClient(newParent);
//
//        // Issue the request.
//        Response r = putEntity(authenticator, getAdminToken());
//        if (shouldSucceed()) {
//            assertErrorResponse(r, Status.BAD_REQUEST);
//        } else {
//            assertErrorResponse(r, Status.NOT_FOUND);
//        }
//    }
//
//    /**
//     * Assert that we cannot update to an invalid authenticator type.
//     *
//     * @throws Exception Exception encountered during test.
//     */
//    @Test
//    public void testPutInvalidateType() throws Exception {
//        Authenticator entity = getEntity(getSecondaryContext());
//        entity.setType("invalid_type");
//
//        // Issue the request.
//        Response r = putEntity(entity, getAdminToken());
//        if (shouldSucceed()) {
//            assertErrorResponse(r, Status.BAD_REQUEST);
//        } else {
//            assertErrorResponse(r, Status.NOT_FOUND);
//        }
//    }

    /**
     * Assert that we never get a password or salt from a GET request.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testGetNoPasswordSalt() throws Exception {
        EnvironmentBuilder builder = getAdminContext()
                .user()
                .login("foo", "bar");
        UserIdentity testingEntity = builder.getUserIdentity();

        Assert.assertNotNull(testingEntity.getPassword());
        Assert.assertNotNull(testingEntity.getSalt());

        // Issue the request.
        Response r = getEntity(testingEntity, getAdminToken());

        if (shouldSucceed()) {
            UserIdentity response = r.readEntity(UserIdentity.class);
            Assert.assertEquals(testingEntity.getId(), response.getId());
            Assert.assertNull(response.getPassword());
            Assert.assertNull(response.getSalt());
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we never get a password or salt from a POST request.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoPasswordSalt() throws Exception {
        UserIdentity testEntity = createValidEntity(getAdminContext());

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        if (shouldSucceed()) {
            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), getAdminToken());
            UserIdentity response = getResponse.readEntity(UserIdentity.class);
            Assert.assertNull(response.getPassword());
            Assert.assertNull(response.getSalt());
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that a created user has their password salted and hashed.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostEncryptPassword() throws Exception {
        UserIdentity testEntity = createValidEntity(getAdminContext());

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        if (shouldSucceed()) {
            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), getAdminToken());
            UserIdentity response = getResponse.readEntity(UserIdentity.class);

            Assert.assertNull(response.getPassword());
            Assert.assertNull(response.getSalt());

            // Resolve the entity from the session so we get all the bits.
            UserIdentity dbEntity = getSession().get(UserIdentity.class,
                    response.getId());

            Assert.assertNotNull(dbEntity.getPassword());
            Assert.assertNotNull(dbEntity.getSalt());
            Assert.assertEquals(dbEntity.getPassword(),
                    PasswordUtil.hash(testEntity.getPassword(),
                            dbEntity.getSalt()));
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that we cannot create an identity for a non-password
     * authenticator.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostWrongAuthenticator() throws Exception {
        EnvironmentBuilder context = getAdminContext();
        context.authenticator("not_password");
        UserIdentity testEntity = createValidEntity(context);
        testEntity.setAuthenticator(context.getAuthenticator());

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that we cannot create an identity with an invalid
     * authenticator.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostInvalidAuthenticator() throws Exception {
        UserIdentity testEntity = createValidEntity(getAdminContext());
        Authenticator invalid = new Authenticator();
        invalid.setId(UUID.randomUUID());
        testEntity.setAuthenticator(invalid);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that we cannot create an identity without an authenticator.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoAuthenticator() throws Exception {
        UserIdentity testEntity = createValidEntity(getAdminContext());
        testEntity.setAuthenticator(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that we cannot create an identity without a user.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostInvalidUser() throws Exception {
        UserIdentity testEntity = createValidEntity(getAdminContext());
        User invalidUser = new User();
        invalidUser.setId(UUID.randomUUID());
        testEntity.setUser(invalidUser);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that we cannot create an identity without a user.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoUser() throws Exception {
        UserIdentity testEntity = createValidEntity(getAdminContext());
        testEntity.setUser(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that we cannot create an identity without a remoteID.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoRemoteId() throws Exception {
        UserIdentity testEntity = createValidEntity(getAdminContext());
        testEntity.setRemoteId(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that we cannot create an identity without a password.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoPassword() throws Exception {
        UserIdentity testEntity = createValidEntity(getAdminContext());
        testEntity.setPassword(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that we never get a password or salt from a PUT request.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutNoPasswordSalt() throws Exception {
        EnvironmentBuilder context = getSecondaryContext();
        UserIdentity testEntity = getEntity(context);

        testEntity.setPassword("OMG PASSWORD");

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());

        if (shouldSucceed()) {
            UserIdentity response = r.readEntity(UserIdentity.class);
            Assert.assertNull(response.getPassword());
            Assert.assertNull(response.getSalt());
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot change the authenticator for an identity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeAuthenticator() throws Exception {
        EnvironmentBuilder context = getSecondaryContext();
        UserIdentity testEntity = getEntity(context);
        Authenticator newAuthenticator = context.authenticator("new")
                .getAuthenticator();

        testEntity.setAuthenticator(newAuthenticator);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot change the owning entity (user).
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeUser() throws Exception {
        EnvironmentBuilder context = getSecondaryContext();
        UserIdentity testEntity = getEntity(context);
        User newUser = context.user()
                .getUser();

        testEntity.setUser(newUser);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
