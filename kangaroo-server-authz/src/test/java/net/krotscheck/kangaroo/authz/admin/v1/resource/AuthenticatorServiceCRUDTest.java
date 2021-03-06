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
import net.krotscheck.kangaroo.authz.admin.v1.exception.InvalidEntityPropertyException;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.MisconfiguredAuthenticatorException;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.hibernate.Session;
import org.junit.Test;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


/**
 * Test the CRUD methods of the Authenticator service.
 *
 * @author Michael Krotscheck
 */
public final class AuthenticatorServiceCRUDTest
        extends AbstractServiceCRUDTest<Authenticator> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<Authenticator>>
            LIST_TYPE = new GenericType<ListResponseEntity<Authenticator>>() {

    };

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
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<Authenticator>> getListType() {
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
        UriBuilder builder = UriBuilder.fromPath("/v1/authenticator/");
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
     * Test that we cannot create an entity with an invalid configuration.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostInvalidConfig() throws Exception {
        Authenticator testEntity = createValidEntity(getAdminContext());
        testEntity.getConfiguration().put("invalid", "bar");

        Response r = postEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, new MisconfiguredAuthenticatorException());
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
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
        assertErrorResponse(r,
                new InvalidEntityPropertyException("client"));
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
        wrongParent.setId(IdUtil.next());
        testEntity.setClient(wrongParent);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r,
                new InvalidEntityPropertyException("client"));
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
        assertErrorResponse(r,
                new InvalidEntityPropertyException("type"));
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
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertEquals(a, response);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }

        // Cleanup the authenticator
        s.getTransaction().begin();
        s.delete(a);
        s.getTransaction().commit();
    }

    /**
     * Assert that a regular entity will error on validation if the
     * configuration is wrong.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutInvalidConfig() throws Exception {
        // Create an entity to update
        Authenticator a = createValidEntity(getSecondaryContext());
        Session s = getSession();
        s.getTransaction().begin();
        s.save(a);
        s.getTransaction().commit();

        // Update the entity
        a.getConfiguration().put("invalid", "config");

        Response r = putEntity(a, getAdminToken());

        if (isAccessible(a, getAdminToken())) {
            assertErrorResponse(r, new MisconfiguredAuthenticatorException());
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

        assertNotEquals(newParent, entity.getClient());

        Authenticator authenticator = new Authenticator();
        authenticator.setId(entity.getId());
        authenticator.setType(entity.getType());
        authenticator.setClient(newParent);

        // Issue the request.
        Response r = putEntity(authenticator, getAdminToken());
        if (isAccessible(entity, getAdminToken())) {
            assertErrorResponse(r,
                    new InvalidEntityPropertyException("client"));
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
            assertErrorResponse(r,
                    new InvalidEntityPropertyException("type"));
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
