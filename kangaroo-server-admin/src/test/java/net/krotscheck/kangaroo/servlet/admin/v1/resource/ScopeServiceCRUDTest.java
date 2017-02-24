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

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;

/**
 * Test the CRUD methods of the scope service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class ScopeServiceCRUDTest
        extends DAbstractServiceCRUDTest<ApplicationScope> {

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
     * Test parameters.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(new Object[][]{
                {
                        ClientType.Implicit,
                        Scope.SCOPE_ADMIN,
                        false,
                        true
                },
                {
                        ClientType.Implicit,
                        Scope.SCOPE,
                        false,
                        true
                },
                {
                        ClientType.Implicit,
                        Scope.SCOPE_ADMIN,
                        true,
                        true
                },
                {
                        ClientType.Implicit,
                        Scope.SCOPE,
                        true,
                        false
                },
                {
                        ClientType.ClientCredentials,
                        Scope.SCOPE_ADMIN,
                        false,
                        true
                },
                {
                        ClientType.ClientCredentials,
                        Scope.SCOPE,
                        false,
                        false
                }
        });
    }
//
//    /**
//     * Load data fixtures for each test.
//     *
//     * @return A list of fixtures, which will be cleared after the test.
//     * @throws Exception An exception that indicates a failed fixture load.
//     */
//    @Override
//    public List<EnvironmentBuilder> fixtures(final EnvironmentBuilder adminApp)
//            throws Exception {
//        // Build the admin context with the provided parameters.
//        EnvironmentBuilder context = getAdminContext();
//        context.client(clientType);
//        if (createUser) {
//            context.user().identity();
//        }
//        adminAppToken = context.bearerToken(tokenScope).getToken();
//
//        // Build a second app to run some tests against.
//        otherApp = new EnvironmentBuilder(getSession())
//                .scopes(Scope.allScopes())
//                .owner(context.getOwner())
//                .client(clientType)
//                .authenticator("test")
//                .user().identity()
//                .bearerToken(tokenScope);
//
//        List<EnvironmentBuilder> fixtures = new ArrayList<>();
//        fixtures.add(otherApp);
//        return fixtures;
//    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected String getUrlForId(final String id) {
        if (id == null) {
            return "/scope/";
        }
        return String.format("/scope/%s", id);
    }

    /**
     * Return the correct testingEntity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    @Override
    protected ApplicationScope getEntity(final EnvironmentBuilder context) {
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
            final EnvironmentBuilder context) {
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
            Assert.assertEquals(HttpStatus.SC_CONFLICT, r.getStatus());
            Assert.assertEquals("conflict", response.getError());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
            Assert.assertEquals("bad_request", response.getError());
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
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertEquals("bad_request", response.getError());
    }

    /**
     * Assert that a scope cannot be created which overwrites another scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostOverwrite() throws Exception {
        EnvironmentBuilder otherApp = getSecondaryContext();
        ApplicationScope newScope = new ApplicationScope();
        newScope.setId(otherApp.getScope().getId());
        newScope.setName(RandomStringUtils.random(20));
        newScope.setApplication(otherApp.getScope().getApplication());

        // Issue the request.
        Response r = postEntity(newScope, getAdminToken());

        ErrorResponse response = r.readEntity(ErrorResponse.class);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertEquals("bad_request", response.getError());
    }

    /**
     * Test a really long name.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooLongName() throws Exception {
        EnvironmentBuilder context = getAdminContext();

        ApplicationScope newScope = new ApplicationScope();
        newScope.setName(RandomStringUtils.randomAlphanumeric(257));
        newScope.setApplication(context.getApplication());

        // Issue the request.
        Response r = postEntity(newScope, getAdminToken());

        ErrorResponse response = r.readEntity(ErrorResponse.class);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertEquals("bad_request", response.getError());
    }

    /**
     * Test that a scope can be created for another application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostApplicationAssign() throws Exception {

        OAuthToken token = getAdminToken();
        EnvironmentBuilder yetAnotherApp = new EnvironmentBuilder(getSession());

        ApplicationScope newScope = new ApplicationScope();
        newScope.setName(RandomStringUtils.randomAlphanumeric(20));
        newScope.setApplication(yetAnotherApp.getApplication());

        // Issue the request.
        Response r = postEntity(newScope, token);

        if (token.getScopes().keySet().contains(getAdminScope())) {
            Assert.assertEquals(HttpStatus.SC_CREATED, r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), token);
            ApplicationScope response =
                    getResponse.readEntity(ApplicationScope.class);
            Assert.assertNotNull(response.getId());
            Assert.assertEquals(newScope.getName(), response.getName());
            Assert.assertEquals(yetAnotherApp.getApplication(),
                    response.getApplication());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
            Assert.assertEquals("bad_request", response.getError());
        }

        yetAnotherApp.clear();
    }

    /**
     * Assert that an admin scope cannot be updated.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutAdminScope() throws Exception {
        ApplicationScope scope = getAdminContext().getScope();
        scope.setName("New Name");

        Response r = putEntity(scope, getAdminToken());

        if (shouldSucceed()) {
            Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
            ErrorResponse result = r.readEntity(ErrorResponse.class);
            Assert.assertEquals("forbidden", result.getError());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
            Assert.assertEquals("not_found", response.getError());
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
        ApplicationScope a = getSecondaryContext().getScope();
        a.setName("Test New Name");
        Response r = putEntity(a, getAdminToken());

        if (shouldSucceed()) {
            ApplicationScope response = r.readEntity(ApplicationScope.class);
            Assert.assertEquals(HttpStatus.SC_OK, r.getStatus());
            Assert.assertEquals(a, response);
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
            Assert.assertEquals("not_found", response.getError());
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

        if (shouldSucceed()) {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
            Assert.assertEquals("bad_request", response.getError());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
            Assert.assertEquals("not_found", response.getError());
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
        EnvironmentBuilder context = getAdminContext();

        // Issue the request.
        Response r = deleteEntity(context.getScope(), getAdminToken());

        if (shouldSucceed()) {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
            Assert.assertEquals("forbidden", response.getError());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
            Assert.assertEquals("not_found", response.getError());
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
        // Issue the request.
        Response r = deleteEntity(getSecondaryContext().getScope(),
                getAdminToken());

        if (shouldSucceed()) {
            Assert.assertEquals(HttpStatus.SC_NO_CONTENT, r.getStatus());
        } else {
            ErrorResponse response = r.readEntity(ErrorResponse.class);
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
            Assert.assertEquals("not_found", response.getError());
        }
    }
}
