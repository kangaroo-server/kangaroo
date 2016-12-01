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

import net.krotscheck.kangaroo.common.exception.exception.HttpNotFoundException;
import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpStatus;
import org.glassfish.hk2.api.ServiceLocator;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests for our common service abstractions.
 *
 * @author Michael Krotscheck
 */
public final class AbstractServiceTest extends AbstractResourceTest {

    /**
     * The mock security context constructed for this test.
     */
    private SecurityContext mockContext;

    /**
     * The abstract service under test.
     */
    private AbstractService service;

    /**
     * A user application from which we can issue tokens.
     */
    private EnvironmentBuilder userApp;

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return null;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return null;
    }

    /**
     * Provided the admin context, build a list of all additional
     * applications required for this test.
     *
     * @param session The session to use to build the environment.
     * @param adminContext The admin context
     * @return A list of fixtures.
     * @throws Exception Thrown if something untoward happens.
     */
    @Override
    public List<EnvironmentBuilder> fixtures(
            final Session session,
            final EnvironmentBuilder adminContext)
            throws Exception {

        // Create a mock context.
        mockContext = Mockito.mock(SecurityContext.class);

        // Create a service.
        service = new TestService();
        service.setConfig(getSystemConfig());
        service.setSession(getSession());
        service.setFullTextSession(getFullTextSession());
        service.setSearchFactory(getSearchFactory());
        service.setSecurityContext(mockContext);

        // Create a 'different' app from which we can issue tokens.
        userApp = new EnvironmentBuilder(session)
                .client(ClientType.Implicit)
                .authenticator("password")
                .user()
                .role("foo")
                .identity("test_identity")
                .bearerToken();

        List<EnvironmentBuilder> fixtures = new ArrayList<>();
        fixtures.add(userApp);
        return fixtures;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected String getUrlForId(final String id) {
        return String.format("/application/%s", id);
    }

    /**
     * Ensure the constructor behaves as expected.
     */
    @Test
    public void testConstructor() {
        Configuration config = Mockito.mock(Configuration.class);
        UriInfo info = Mockito.mock(UriInfo.class);
        Session session = Mockito.mock(Session.class);
        ServiceLocator serviceLocator = Mockito.mock(ServiceLocator.class);
        SearchFactory factory = Mockito.mock(SearchFactory.class);
        FullTextSession ftSession = Mockito.mock(FullTextSession.class);
        SecurityContext c = Mockito.mock(SecurityContext.class);
        AbstractService s = new TestService();
        s.setServiceLocator(serviceLocator);
        s.setConfig(config);
        s.setSession(session);
        s.setSearchFactory(factory);
        s.setFullTextSession(ftSession);
        s.setSecurityContext(c);
        s.setUriInfo(info);

        Assert.assertEquals(serviceLocator, s.getServiceLocator());
        Assert.assertEquals(session, s.getSession());
        Assert.assertEquals(factory, s.getSearchFactory());
        Assert.assertEquals(ftSession, s.getFullTextSession());
        Assert.assertEquals(c, s.getSecurityContext());
        Assert.assertEquals(config, s.getConfig());
        Assert.assertEquals(info, s.getUriInfo());
    }

    /**
     * Assert that we can get the current user principal.
     */
    @Test
    public void testGetCurrentUser() {
        EnvironmentBuilder adminContext = getAdminContext();
        adminContext.bearerToken();

        Mockito.doReturn(getAdminContext().getToken())
                .when(mockContext).getUserPrincipal();

        User user = service.getCurrentUser();
        Assert.assertNotNull(user);
        Assert.assertEquals(adminContext.getToken().getIdentity().getUser(),
                user);
    }

    /**
     * Assert that a token issued to a client credential user does not return
     * a user.
     */
    @Test
    public void testGetCurrentUserClientCredentials() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.ClientCredentials)
                .bearerToken();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();

        Assert.assertNull(service.getCurrentUser());
    }

    /**
     * Assert that we can get the current admin application.
     */
    @Test
    public void testGetAdminApplication() {
        Application a = service.getAdminApplication();
        Assert.assertEquals(getAdminContext().getApplication(), a);
    }

    /**
     * Assert that a nonexistent entity will show up as not found.
     */
    @Test(expected = HttpNotFoundException.class)
    public void testAssertCanAccessNull() {
        service.assertCanAccess(null, Scope.APPLICATION_ADMIN);
    }

    /**
     * Assert that a current owner of an entity can access it.
     */
    @Test
    public void testAssertCanAccessAsOwner() {
        // Add a new, non-admin user, and make it the owner of the user app
        EnvironmentBuilder adminContext = getAdminContext()
                .role("lol")
                .user()
                .identity("not_admin")
                .bearerToken();
        userApp.owner(adminContext.getUser());

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();

        service.assertCanAccess(userApp.getApplication(),
                Scope.APPLICATION_ADMIN);
        Assert.assertTrue(true);
    }

    /**
     * Assert that a non owner of an entity can access it if they have the
     * correct scope.
     */
    @Test
    public void testAssertCanAccessNotOwnerValidScope() {
        EnvironmentBuilder adminContext = getAdminContext()
                .bearerToken(Scope.APPLICATION_ADMIN);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userApp.getApplication(),
                Scope.APPLICATION_ADMIN);
        Assert.assertTrue(true);
    }

    /**
     * Assert that a non owner of an entity cannot access it if they have the
     * incorrect scope.
     */
    @Test(expected = HttpNotFoundException.class)
    public void testAssertCanAccessNotOwnerInvalidScope() {
        EnvironmentBuilder adminContext = getAdminContext()
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(false).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userApp.getApplication(),
                Scope.APPLICATION_ADMIN);
    }

    /**
     * Assert that a client credentials token can access the entity if they
     * have the admin scope.
     */
    @Test
    public void testAssertCanAccessClientCredentialsValidScope() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.ClientCredentials)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION_ADMIN);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userApp.getApplication(),
                Scope.APPLICATION_ADMIN);
        Assert.assertTrue(true);
    }

    /**
     * Assert that a client credentials token cannot access the entity if they
     * do not have the admin scope.
     */
    @Test(expected = HttpNotFoundException.class)
    public void testAssertCanAccessClientCredentialsInvalidScope() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.ClientCredentials)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(false).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userApp.getApplication(),
                Scope.APPLICATION_ADMIN);
    }

    /**
     * Assert that an admin, when passing an empty openID, returns null.
     */
    @Test
    public void testRequestUserFilterAdminNoFilter() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        User user = service.resolveOwnershipFilter(null);
        Assert.assertNull(user);
    }

    /**
     * Assert that an admin, when passing an invalid ownerID, gets an error.
     */
    @Test(expected = HttpStatusException.class)
    public void testRequestInvalidUserFilterAdminFilter() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .user();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.resolveOwnershipFilter(UUID.randomUUID());
    }

    /**
     * Assert that an admin, when passing an ownerID, gets the owner ID.
     */
    @Test
    public void testRequestUserFilterAdminFilter() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .user();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        User requestedUser = adminContext.getUser();
        User user = service.resolveOwnershipFilter(requestedUser.getId());
        Assert.assertEquals(requestedUser, user);
    }

    /**
     * Assert that a non admin defaults to self-filtering.
     */
    @Test
    public void testRequestUserFilterNonAdminNoFilter() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        User requestedUser = adminContext.getUser();
        User user = service.resolveOwnershipFilter(null);
        Assert.assertEquals(adminContext.getUser(), user);
    }

    /**
     * Assert that a non admin cannot filter by a different user.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRequestUserFilterNonAdminFilter() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .user();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        User requestedUser = adminContext.getUser();
        service.resolveOwnershipFilter(requestedUser.getId());
    }

    /**
     * Assert that a non admin can filter by themselves.
     */
    @Test
    public void testRequestUserFilterNonAdminFilterSelf() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        User requestedUser = adminContext.getToken().getIdentity().getUser();
        User user = service.resolveOwnershipFilter(requestedUser.getId());
        Assert.assertEquals(requestedUser, user);
    }

    /**
     * Assert that a client credentials client cannot filter without the
     * appropriate scope.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRequestUserFilterNonAdminClientCredentials() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.ClientCredentials)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveOwnershipFilter(adminContext.getUser().getId());
    }

    /**
     * Assert that we can require a null entity.
     */
    @Test(expected = HttpStatusException.class)
    public void testRequireEntityInputNullEntity() {
        service.requireEntityInput(Application.class, null);
    }

    /**
     * Assert that we can require an entity with a null ID.
     */
    @Test(expected = HttpStatusException.class)
    public void testREquireEntityInputNoIdEntity() {
        service.requireEntityInput(Application.class, new Application());
    }

    /**
     * Assert that we can require a valid entity
     */
    @Test
    public void testRequireValidEntity() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        Application a =
                service.requireEntityInput(Application.class,
                        userApp.getApplication());
        Assert.assertEquals(userApp.getApplication(), a);
    }

    /**
     * Assert that passing no entity ID as an admin returns null.
     */
    @Test
    public void testResolveFilterEntityNoEntityAdmin() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        Application result =
                service.resolveFilterEntity(Application.class, null);
        Assert.assertNull(result);
    }

    /**
     * Assert that passing a nonexitent entity ID as an admin throws a 404.
     */
    @Test(expected = HttpStatusException.class)
    public void testResolveFilterEntityNonexistentEntityIdAdmin() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.resolveFilterEntity(Application.class, UUID.randomUUID());
    }

    /**
     * Assert that passing a valid entity ID as an admin, we get the entity.
     */
    @Test
    public void testResolveFilterEntityValidEntityIdAdmin() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        Application a =
                service.resolveFilterEntity(Application.class,
                        userApp.getApplication().getId());
        Assert.assertEquals(userApp.getApplication(), a);
    }

    /**
     * Assert that an invalidly scoped token throws an exception.
     */
    @Test(expected = InvalidScopeException.class)
    public void testResolveFilterEntityInvalidScope() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.USER);

        service.resolveFilterEntity(Application.class,
                userApp.getApplication().getId());
    }

    /**
     * Assert that passing a null entity ID as a regular user returns null.
     */
    @Test
    public void testResolveFilterEntityNullEntityIdNonAdmin() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        Application a = service.resolveFilterEntity(Application.class, null);
        Assert.assertNull(a);
    }

    /**
     * Assert that passing a nonexitent entity ID as a regular user throws an
     * invalidScope exception.
     */
    @Test(expected = HttpStatusException.class)
    public void testResolveFilterEntityNonexistentEntityIdNonAdmin() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveFilterEntity(Application.class, UUID.randomUUID());
    }

    /**
     * Assert that trying to filter on an entity with a token that has no
     * user identity and a regular scope throws an exception.
     */
    @Test(expected = InvalidScopeException.class)
    public void testResolveFilterEntityNoTokenIdentity() {
        EnvironmentBuilder adminContext = getAdminContext()
                .client(ClientType.ClientCredentials)
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveFilterEntity(Application.class,
                adminContext.getApplication().getId());
    }

    /**
     * Assert that trying to filter on an entity that the user
     * doesn't own fails for the non-admin scope.
     */
    @Test(expected = HttpStatusException.class)
    public void testResolveFilterEntityNonAdminNonOwner() {
        // Set up an owner for the user app.
        EnvironmentBuilder adminContext = getAdminContext()
                .user().identity();
        userApp.owner(adminContext.getUser());

        // Add a user for the token.
        adminContext.user().identity()
                .bearerToken(Scope.APPLICATION);

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveFilterEntity(Application.class,
                userApp.getApplication().getId());
    }

    /**
     * Assert that a regular user can filter on an entity if they are the owner.
     */
    @Test
    public void testResolveFilterEntityNonAdminOwner() {
        EnvironmentBuilder adminContext = getAdminContext()
                .user().identity()
                .bearerToken(Scope.APPLICATION);
        userApp.owner(adminContext.getUser());

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        Application a =
                service.resolveFilterEntity(Application.class,
                        userApp.getApplication().getId());
        Assert.assertEquals(adminContext.getUser(), a.getOwner());
    }

    /**
     * Assert that we can create and execute a query on a given type.
     */
    @Test
    public void testCreateQuery() {
        FullTextQuery q = service.buildQuery(
                Application.class,
                new String[]{"name"},
                "kangaroo");
        Assert.assertEquals("name:kangaroo~2", q.getQueryString());

        Response results = service.executeQuery(q, 0, 10);
        Assert.assertEquals(HttpStatus.SC_OK, results.getStatus());
        Assert.assertEquals("1", results.getHeaderString("Total"));
        Assert.assertEquals("0", results.getHeaderString("Offset"));
        Assert.assertEquals("10", results.getHeaderString("Limit"));
    }

    /**
     * Concrete implementation of the abstract service, for testing.
     */
    public static final class TestService extends AbstractService {

        /**
         * Return the admin scope.
         *
         * @return The admin scope...
         */
        @Override
        protected String getAdminScope() {
            return Scope.APPLICATION_ADMIN;
        }

        /**
         * Return the application scope.
         *
         * @return The application scope!
         */
        @Override
        protected String getAccessScope() {
            return Scope.APPLICATION;
        }
    }
}
