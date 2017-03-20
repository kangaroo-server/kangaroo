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

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.test.ApplicationBuilder;
import net.krotscheck.kangaroo.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.HttpUtil;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

/**
 * Test the CRUD methods of the scope service.
 *
 * @param <T> The type of entity to execute this test for.
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public abstract class AbstractServiceCRUDTest<T extends AbstractEntity>
        extends AbstractResourceTest {

    /**
     * Class reference for this class' type, used in casting.
     */
    private final Class<T> typingClass;

    /**
     * The scope to grant the issued token.
     */
    private final String tokenScope;

    /**
     * The type of OAuth2 client to create.
     */
    private final ClientType clientType;

    /**
     * The client under test.
     */
    private Client client;

    /**
     * Whether to create a user, or fall back on an existing user. In most
     * cases, this will fall back to the owner of the admin scope.
     */
    private final Boolean createUser;

    /**
     * Do we expect the result to be successful, or rejected?
     */
    private final Boolean shouldSucceed;

    /**
     * The token issued to the admin app, with appropriate credentials.
     */
    private OAuthToken adminAppToken;

    /**
     * Create a new instance of this parameterized test.
     *
     * @param typingClass   The raw class type, used for type-based parsing.
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public AbstractServiceCRUDTest(final Class<T> typingClass,
                                   final ClientType clientType,
                                   final String tokenScope,
                                   final Boolean createUser,
                                   final Boolean shouldSucceed) {
        this.typingClass = typingClass;
        this.tokenScope = tokenScope;
        this.clientType = clientType;
        this.createUser = createUser;
        this.shouldSucceed = shouldSucceed;
    }

    /**
     * Load data fixtures for each test. Here we're creating two applications
     * with different owners, using the kangaroo default scopes so we have
     * some good cross-app name duplication.
     *
     * @throws Exception An exception that indicates a failed fixture load.
     */
    @Before
    public final void configureData() throws Exception {

        // Get the admin app and create users based on the configured
        // parameters.
        ApplicationContext context = getAdminContext();
        User owner = context.getOwner();

        client = context.getBuilder()
                .client(clientType)
                .build()
                .getClient();

        if (createUser) {
            // Switch to the other user.
            owner = getSecondaryContext().getOwner();
        }
        UserIdentity identity = owner.getIdentities().iterator().next();

        adminAppToken = context
                .getBuilder()
                .bearerToken(client, identity, tokenScope)
                .build()
                .getToken();
    }

    /**
     * Return the correct testingEntity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    protected abstract T getEntity(ApplicationContext context);

    /**
     * Return a new, empty entity.
     *
     * @return The requested entity type under test.
     */
    protected abstract T getNewEntity();

    /**
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    protected abstract T createValidEntity(ApplicationContext context);

    /**
     * Return the oauth token for the primary application.
     *
     * @return The application token.
     */
    protected final OAuthToken getAdminToken() {
        return adminAppToken;
    }

    /**
     * Return the oauth token for the secondary application.
     *
     * @return The application token.
     */
    protected final OAuthToken getSecondaryToken() {
        return getSecondaryContext().getToken();
    }

    /**
     * Return the client under test.
     *
     * @return The client under test.
     */
    public final Client getAdminClient() {
        return client;
    }

    /**
     * Return whether this test should succeed.
     *
     * @return The application token.
     */
    protected final Boolean shouldSucceed() {
        return shouldSucceed;
    }

    /**
     * Return the configured token scope for this test.
     *
     * @return The token scope.
     */
    public final String getTokenScope() {
        return tokenScope;
    }

    /**
     * Return the configured client type.
     *
     * @return The client type.
     */
    public final ClientType getClientType() {
        return clientType;
    }

    /**
     * Return true if the current test parameters indicate a Client
     * Credentials-based client without any admin credentials. These types of
     * clients have no identity assigned to them, and therefore cannot 'own'
     * any resources which they could then access.
     *
     * @return True if this is a Client Credentials client without admin scope.
     */
    protected final Boolean isLimitedByClientCredentials() {
        return clientType.equals(ClientType.ClientCredentials)
                && tokenScope.equals(getRegularScope());
    }

    /**
     * Assert that admin entity can be read, if accessed from a scope issued
     * via the admin app.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testGetAdmin() throws Exception {
        T testingEntity = getEntity(getAdminContext());

        // Issue the request.
        Response r = getEntity(testingEntity, adminAppToken);

        if (shouldSucceed) {
            T response = r.readEntity(typingClass);
            Assert.assertEquals(testingEntity, response);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a non-admin entity can be read, from the admin app, with
     * appropriate credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testGetRegularApp() throws Exception {
        T testingEntity = getEntity(getSecondaryContext());

        // Issue the request.
        Response r = getEntity(testingEntity, adminAppToken);

        if (isAccessible(testingEntity, adminAppToken)) {
            T response = r.readEntity(typingClass);
            Assert.assertEquals(testingEntity, response);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that an entity cannot be read if the token is issued to a
     * different app.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testGetFromExternalApp() throws Exception {
        T testingEntity = getEntity(getAdminContext());
        OAuthToken token = getSecondaryContext().getToken();

        // Issue the request.
        Response r = getEntity(testingEntity, token);

        assertErrorResponse(r, Status.FORBIDDEN);
    }

    /**
     * Assert that an entity can not be read, if accessed without
     * credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testGetByUnknown() throws Exception {
        T testingEntity = getEntity(getAdminContext());

        // Issue the request.
        Response r = getEntity(testingEntity, null);

        assertErrorResponse(r, Status.FORBIDDEN);
    }

    /**
     * Assert that a malformed ID errors as expected.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testGetMalformedId() throws Exception {
        Response r = getEntity("malformed_id",
                HttpUtil.authHeaderBearer(adminAppToken.getId().toString()));

        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that a nonexistent ID gives a 404.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testGetNonexistent() throws Exception {
        Response r = getEntity(UUID.randomUUID().toString(),
                HttpUtil.authHeaderBearer(adminAppToken.getId().toString()));

        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that an entity can be created.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPost() throws Exception {
        T testEntity = createValidEntity(getAdminContext());

        // Issue the request.
        Response r = postEntity(testEntity, adminAppToken);

        if (shouldSucceed()) {
            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), adminAppToken);
            T response = getResponse.readEntity(typingClass);
            assertContentEquals(testEntity, response);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that a request with a null body fails.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPostNoBody() throws Exception {
        // Issue the request.
        Response r = postEntity(null, adminAppToken);
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that an entity cannot be created with a provided ID.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPostWithId() throws Exception {
        T testEntity = createValidEntity(getAdminContext());
        testEntity.setId(UUID.randomUUID());

        // Issue the request.
        Response r = postEntity(testEntity, adminAppToken);
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that some users may create entities for other parents.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPostOtherParent() throws Exception {
        T testEntity = createValidEntity(getSecondaryContext());

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());

        if (this.isAccessible(testEntity, getAdminToken())) {
            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), getAdminToken());
            T response = getResponse.readEntity(typingClass);
            Assert.assertNotNull(response.getId());
            assertContentEquals(testEntity, response);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that you can't post a null entity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPostNoEntity() throws Exception {
        // Issue the request.
        Response r = postEntity(null, adminAppToken);
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that an entity cannot be created with credentials from another
     * app.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPostExternalCredentials() throws Exception {
        T testEntity = createValidEntity(getAdminContext());

        // Issue the request.
        Response r = postEntity(testEntity, getSecondaryContext().getToken());
        assertErrorResponse(r, Status.FORBIDDEN);
    }

    /**
     * Test that an entity can/not be created for another context.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPostDifferentApplication() throws Exception {
        ApplicationContext thirdApp = ApplicationBuilder
                .newApplication(getSession())
                .client(getClientType())
                .redirect("http://third.example.org/redirect")
                .referrer("http://third.example.org/referrer")
                .authenticator("password")
                .user()
                .identity()
                .build();
        T testingEntity = createValidEntity(thirdApp);

        // Issue the request.
        Response r = postEntity(testingEntity, adminAppToken);

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else if (testingEntity instanceof Application
                && adminAppToken.getIdentity() == null) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else if (tokenScope.equals(getAdminScope())
                || testingEntity instanceof Application) {
            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), adminAppToken);

            T responseEntity = getResponse.readEntity(typingClass);
            Assert.assertNotNull(responseEntity.getId());
            Assert.assertNotNull(responseEntity.getCreatedDate());
            Assert.assertNotNull(responseEntity.getModifiedDate());
            assertContentEquals(testingEntity, responseEntity);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that an entity cannot be created with an unknown user.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPostByUnknown() throws Exception {
        // Create a valid entity.
        T testingEntity = createValidEntity(getAdminContext());

        // Issue the request.
        Response r = postEntity(testingEntity, (OAuthToken) null);

        assertErrorResponse(r, Status.FORBIDDEN);
    }

    /**
     * Assert that a regular scope cannot have its id changed.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPutChangeId() throws Exception {
        // Create an entity to test with
        Session s = getSession();
        T testEntity = createValidEntity(getSecondaryContext());
        s.getTransaction().begin();
        s.save(testEntity);
        s.getTransaction().commit();

        // Evict so we can use it.
        s.evict(testEntity);

        UUID oldId = testEntity.getId();
        testEntity.setId(UUID.randomUUID());

        // Issue the request.
        Response r = putEntity(oldId.toString(), testEntity,
                HttpUtil.authHeaderBearer(adminAppToken.getId()));

        if (this.isAccessible(testEntity, adminAppToken)) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that an entity cannot be updated if the token is issued to a
     * different app.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPutFromExternalApp() throws Exception {
        T testingEntity = getEntity(getSecondaryContext());
        OAuthToken token = getSecondaryContext().getToken();

        // Issue the request.
        Response r = putEntity(testingEntity, token);
        assertErrorResponse(r, Status.FORBIDDEN);
    }

    /**
     * Assert that an entity can not be updated, if accessed without
     * credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPutByUnknown() throws Exception {
        T testingEntity = getEntity(getSecondaryContext());
        Response r = putEntity(testingEntity, (OAuthToken) null);
        assertErrorResponse(r, Status.FORBIDDEN);
    }

    /**
     * Assert that a malformed ID causes no changes.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPutMalformedId() throws Exception {
        Response r = putEntity("malformed_id", getNewEntity(),
                HttpUtil.authHeaderBearer(adminAppToken.getId()));

        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that a nonexistent ID causes no changes.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPutNonexistent() throws Exception {
        T testingEntity = getNewEntity();
        testingEntity.setId(UUID.randomUUID());
        Response r = putEntity(testingEntity, adminAppToken);
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that a regular entity can be deleted, from the admin app, with
     * appropriate credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testDeleteRegularEntity() throws Exception {
        // Create an entity to delete.
        T testingEntity = createValidEntity(getSecondaryContext());

        Session s = getSession();
        s.getTransaction().begin();
        s.save(testingEntity);
        s.getTransaction().commit();

        // Issue the request.
        Response r = deleteEntity(testingEntity, adminAppToken);

        if (isAccessible(testingEntity, adminAppToken)) {
            Assert.assertEquals(Status.NO_CONTENT.getStatusCode(),
                    r.getStatus());
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a scope cannot be deleted if the token is issued to a
     * different app.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testDeleteFromExternalApp() throws Exception {
        T testingEntity = getEntity(getAdminContext());

        // Issue the request.
        Response r = deleteEntity(testingEntity,
                getSecondaryContext().getToken());
        assertErrorResponse(r, Status.FORBIDDEN);
    }

    /**
     * Assert that an scope can not be deleted, if accessed without
     * credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testDeleteByUnknown() throws Exception {
        // Create an entity to delete.
        T testingEntity = createValidEntity(getSecondaryContext());

        Session s = getSession();
        s.getTransaction().begin();
        s.save(testingEntity);
        s.getTransaction().commit();

        Response r = deleteEntity(testingEntity, null);
        assertErrorResponse(r, Status.FORBIDDEN);
    }

    /**
     * Assert that a malformed ID causes no changes.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testDeleteMalformedId() throws Exception {
        Response r = deleteEntity("malformed_id",
                HttpUtil.authHeaderBearer(adminAppToken.getId()));
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that a nonexistent ID causes no changes.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testDeleteNonexistent() throws Exception {
        Response r = deleteEntity(UUID.randomUUID().toString(),
                HttpUtil.authHeaderBearer(adminAppToken.getId()));
        assertErrorResponse(r, Status.NOT_FOUND);
    }
}
