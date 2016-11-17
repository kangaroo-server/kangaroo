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
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.util.PasswordUtil;
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
    protected URI getUrlForId(final String id) {
        UriBuilder builder = UriBuilder.fromPath("/identity/");
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
    protected UserIdentity getEntity(final ApplicationContext context) {
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
    protected UserIdentity createValidEntity(final ApplicationContext context) {
        UserIdentity identity = new UserIdentity();
        identity.setRemoteId(UUID.randomUUID().toString());
        identity.setPassword(UUID.randomUUID().toString());
        identity.setUser(context.getUser());
        identity.getClaims().put("foo", "bar");
        identity.getClaims().put("lol", "cat");

        // Find an appropriate authenticator
        Authenticator authenticator = getAttached(context.getApplication())
                .getClients()
                .stream()
                .flatMap((c) -> c.getAuthenticators().stream())
                .filter((a -> a.getType().equals(AuthenticatorType.Password)))
                .collect(Collectors.toList())
                .get(0);

        identity.setType(authenticator.getType());
        return identity;
    }

    /**
     * Assert that we never get a password or salt from a GET request.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testGetNoPasswordSalt() throws Exception {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .user()
                .login("foo", "bar")
                .build();
        UserIdentity testingEntity = testContext.getUserIdentity();

        Assert.assertNotNull(testingEntity.getPassword());
        Assert.assertNotNull(testingEntity.getSalt());

        // Issue the request.
        Response r = getEntity(testingEntity, getAdminToken());

        if (shouldSucceed()) {
            Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());

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

            // Load all entities. This seems to add just enough lag into the
            // test that we can correctly load the dbEntity later.
            getSession().createCriteria(UserIdentity.class).list();

            // Resolve the entity from the session so we get all the bits.
            getSession().getTransaction().begin();
            UserIdentity dbEntity = getSession()
                    .byId(UserIdentity.class)
                    .load(response.getId());

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
        ApplicationContext context = getAdminContext()
                .getBuilder()
                .authenticator(AuthenticatorType.Test)
                .build();
        UserIdentity testEntity = createValidEntity(context);
        testEntity.setType(context.getAuthenticator().getType());

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
        testEntity.setType(null);

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
        ApplicationContext context = getSecondaryContext()
                .getBuilder()
                .user()
                .login("test", "password")
                .build();
        UserIdentity testEntity = context.getUserIdentity();

        testEntity.setPassword("OMG PASSWORD");

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());

        if (isAccessible(testEntity, getAdminToken())) {
            Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());

            UserIdentity response = r.readEntity(UserIdentity.class);
            Assert.assertNull(response.getPassword());
            Assert.assertNull(response.getSalt());
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot change the type of an identity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeType() throws Exception {
        ApplicationContext context = getSecondaryContext()
                .getBuilder()
                .authenticator(AuthenticatorType.Test)
                .user()
                .login("test", "password")
                .build();
        UserIdentity testEntity = context.getUserIdentity();

        testEntity.setType(AuthenticatorType.Password);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());

        if (isAccessible(testEntity, getAdminToken())) {
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
        ApplicationContext context = getSecondaryContext()
                .getBuilder()
                .user()
                .login("test", "password")
                .user()
                .build();
        UserIdentity originalEntity = context.getUserIdentity();
        User newUser = context.getUser();

        UserIdentity testEntity = (UserIdentity) originalEntity.clone();
        testEntity.setUser(newUser);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());

        if (isAccessible(originalEntity, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
