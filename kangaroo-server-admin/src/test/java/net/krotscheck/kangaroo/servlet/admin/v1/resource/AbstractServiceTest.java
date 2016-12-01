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
import net.krotscheck.kangaroo.test.ApplicationBuilder;
import net.krotscheck.kangaroo.test.ApplicationBuilder.ApplicationContext;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpStatus;
import org.glassfish.hk2.api.ServiceLocator;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
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
    private SecurityContext mockSecurityContext;

    /**
     * The abstract service under test.
     */
    private AbstractService service;

    /**
     * A user application from which we can issue tokens.
     */
    private ApplicationContext userContext;

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
     * Create mock components.
     */
    @Before
    public void initializeTest() {
        // Create a mock context.
        mockSecurityContext = Mockito.mock(SecurityContext.class);

        // Create a service.
        service = new TestService();
        service.setConfig(TEST_DATA_RESOURCE.getSystemConfiguration());
        service.setSession(getSession());
        service.setFullTextSession(getFullTextSession());
        service.setSearchFactory(getSearchFactory());
        service.setSecurityContext(mockSecurityContext);

        // Create a 'different' app from which we can issue tokens.
        userContext = ApplicationBuilder.newApplication(getSession())
                .client(ClientType.Implicit)
                .authenticator("password")
                .user()
                .role("foo")
                .identity("test_identity")
                .bearerToken()
                .build();
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
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .bearerToken()
                .build();

        Mockito.doReturn(getAdminContext().getToken())
                .when(mockSecurityContext).getUserPrincipal();

        User user = service.getCurrentUser();
        Assert.assertNotNull(user);
        Assert.assertEquals(testContext.getToken().getIdentity().getUser(),
                user);
    }

    /**
     * Assert that a token issued to a client credential user does not return
     * a user.
     */
    @Test
    public void testGetCurrentUserClientCredentials() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.ClientCredentials)
                .bearerToken()
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();

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
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .role("lol")
                .user()
                .identity("not_admin")
                .bearerToken()
                .build();
        ApplicationContext userContext = this.userContext.getBuilder()
                .owner(testContext.getUser())
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();

        service.assertCanAccess(this.userContext.getApplication(),
                Scope.APPLICATION_ADMIN);
        Assert.assertTrue(true);
    }

    /**
     * Assert that a non owner of an entity can access it if they have the
     * correct scope.
     */
    @Test
    public void testAssertCanAccessNotOwnerValidScope() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .bearerToken(Scope.APPLICATION_ADMIN)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userContext.getApplication(),
                Scope.APPLICATION_ADMIN);
        Assert.assertTrue(true);
    }

    /**
     * Assert that a non owner of an entity cannot access it if they have the
     * incorrect scope.
     */
    @Test(expected = HttpNotFoundException.class)
    public void testAssertCanAccessNotOwnerInvalidScope() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(false).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userContext.getApplication(),
                Scope.APPLICATION_ADMIN);
    }

    /**
     * Assert that a client credentials token can access the entity if they
     * have the admin scope.
     */
    @Test
    public void testAssertCanAccessClientCredentialsValidScope() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.ClientCredentials)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION_ADMIN)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userContext.getApplication(),
                Scope.APPLICATION_ADMIN);
        Assert.assertTrue(true);
    }

    /**
     * Assert that a client credentials token cannot access the entity if they
     * do not have the admin scope.
     */
    @Test(expected = HttpNotFoundException.class)
    public void testAssertCanAccessClientCredentialsInvalidScope() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.ClientCredentials)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(false).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userContext.getApplication(),
                Scope.APPLICATION_ADMIN);
    }

    /**
     * Assert that an admin, when passing an empty openID, returns null.
     */
    @Test
    public void testRequestUserFilterAdminNoFilter() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        User user = service.resolveOwnershipFilter(null);
        Assert.assertNull(user);
    }

    /**
     * Assert that an admin, when passing an invalid ownerID, gets an error.
     */
    @Test(expected = HttpStatusException.class)
    public void testRequestInvalidUserFilterAdminFilter() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .user()
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.resolveOwnershipFilter(UUID.randomUUID());
    }

    /**
     * Assert that an admin, when passing an ownerID, gets the owner ID.
     */
    @Test
    public void testRequestUserFilterAdminFilter() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .user()
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        User requestedUser = testContext.getUser();
        User user = service.resolveOwnershipFilter(requestedUser.getId());
        Assert.assertEquals(requestedUser, user);
    }

    /**
     * Assert that a non admin defaults to self-filtering.
     */
    @Test
    public void testRequestUserFilterNonAdminNoFilter() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION);

        User requestedUser = testContext.getUser();
        User user = service.resolveOwnershipFilter(null);
        Assert.assertEquals(testContext.getUser(), user);
    }

    /**
     * Assert that a non admin cannot filter by a different user.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRequestUserFilterNonAdminFilter() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .user()
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION);

        User requestedUser = testContext.getUser();
        service.resolveOwnershipFilter(requestedUser.getId());
    }

    /**
     * Assert that a non admin can filter by themselves.
     */
    @Test
    public void testRequestUserFilterNonAdminFilterSelf() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION);

        User requestedUser = testContext.getToken().getIdentity().getUser();
        User user = service.resolveOwnershipFilter(requestedUser.getId());
        Assert.assertEquals(requestedUser, user);
    }

    /**
     * Assert that a client credentials client cannot filter without the
     * appropriate scope.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRequestUserFilterNonAdminClientCredentials() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.ClientCredentials)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveOwnershipFilter(testContext.getUser().getId());
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
     * Assert that we can require a valid entity.
     */
    @Test
    public void testRequireValidEntity() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        Application a =
                service.requireEntityInput(Application.class,
                        userContext.getApplication());
        Assert.assertEquals(userContext.getApplication(), a);
    }

    /**
     * Assert that passing no entity ID as an admin returns null.
     */
    @Test
    public void testResolveFilterEntityNoEntityAdmin() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
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
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.resolveFilterEntity(Application.class, UUID.randomUUID());
    }

    /**
     * Assert that passing a valid entity ID as an admin, we get the entity.
     */
    @Test
    public void testResolveFilterEntityValidEntityIdAdmin() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        Application a =
                service.resolveFilterEntity(Application.class,
                        userContext.getApplication().getId());
        Assert.assertEquals(userContext.getApplication(), a);
    }

    /**
     * Assert that an invalidly scoped token throws an exception.
     */
    @Test(expected = InvalidScopeException.class)
    public void testResolveFilterEntityInvalidScope() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.USER);

        service.resolveFilterEntity(Application.class,
                userContext.getApplication().getId());
    }

    /**
     * Assert that passing a null entity ID as a regular user returns null.
     */
    @Test
    public void testResolveFilterEntityNullEntityIdNonAdmin() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
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
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.Implicit)
                .authenticator("test")
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveFilterEntity(Application.class, UUID.randomUUID());
    }

    /**
     * Assert that trying to filter on an entity with a token that has no
     * user identity and a regular scope throws an exception.
     */
    @Test(expected = InvalidScopeException.class)
    public void testResolveFilterEntityNoTokenIdentity() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .client(ClientType.ClientCredentials)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveFilterEntity(Application.class,
                testContext.getApplication().getId());
    }

    /**
     * Assert that trying to filter on an entity that the user
     * doesn't own fails for the non-admin scope.
     */
    @Test(expected = HttpStatusException.class)
    public void testResolveFilterEntityNonAdminNonOwner() {
        // Set up an owner for the user app.
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .user()
                .identity()
                .build();
        userContext
                .getBuilder()
                .owner(testContext.getUser())
                .build();

        // Add a user for the token.
        testContext = testContext.getBuilder()
                .user()
                .identity()
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveFilterEntity(Application.class,
                userContext.getApplication().getId());
    }

    /**
     * Assert that a regular user can filter on an entity if they are the owner.
     */
    @Test
    public void testResolveFilterEntityNonAdminOwner() {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .user()
                .identity()
                .bearerToken(Scope.APPLICATION)
                .build();
        userContext
                .getBuilder()
                .owner(testContext.getUser())
                .build();

        Mockito.doReturn(testContext.getToken())
                .when(mockSecurityContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockSecurityContext)
                .isUserInRole(Scope.APPLICATION);

        Application a =
                service.resolveFilterEntity(Application.class,
                        userContext.getApplication().getId());
        Assert.assertEquals(testContext.getUser(), a.getOwner());
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
