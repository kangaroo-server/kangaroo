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
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidScopeException;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.entity.AbstractEntity;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.apache.commons.configuration.Configuration;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mockito;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for our common service abstractions.
 *
 * @author Michael Krotscheck
 */
public final class AbstractServiceTest extends AbstractResourceTest {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<AbstractEntity>>
            LIST_TYPE = new GenericType<ListResponseEntity<AbstractEntity>>() {

    };
    /**
     * A user application from which we can issue tokens.
     */
    private static ApplicationContext userApp;
    /**
     * Test data loading for this test.
     */
    @ClassRule
    public static final TestRule TEST_DATA_RULE =
            new TestDataResource(HIBERNATE_RESOURCE) {
                /**
                 * Initialize the test data.
                 */
                @Override
                protected void loadTestData(final Session session) {
                    userApp = ApplicationBuilder.newApplication(session)
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Password)
                            .user()
                            .role("foo")
                            .identity("test_identity")
                            .bearerToken()
                            .build();
                }
            };
    /**
     * The mock security context constructed for this test.
     */
    private SecurityContext mockContext;
    /**
     * The abstract service under test.
     */
    private AbstractService service;

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<AbstractEntity>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Setup test data.
     */
    @Before
    public void setupTestData() {
        mockContext = Mockito.mock(SecurityContext.class);

        // Create a service.
        service = new TestService();
        service.setConfig(getSystemConfig());
        service.setSession(getSession());
        service.setFullTextSession(getFullTextSession());
        service.setSearchFactory(getSearchFactory());
        service.setSecurityContext(mockContext);
    }

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
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return UriBuilder.fromPath("/application/")
                .path(id)
                .build();
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractAuthzEntity entity) {
        return getUrlForId(IdUtil.toString(entity.getId()));
    }

    /**
     * Ensure the constructor behaves as expected.
     */
    @Test
    public void testConstructor() {
        Configuration config = Mockito.mock(Configuration.class);
        UriInfo info = Mockito.mock(UriInfo.class);
        Session session = Mockito.mock(Session.class);
        InjectionManager injector = Mockito.mock(InjectionManager.class);
        SearchFactory factory = Mockito.mock(SearchFactory.class);
        FullTextSession ftSession = Mockito.mock(FullTextSession.class);
        SecurityContext c = Mockito.mock(SecurityContext.class);
        AbstractService s = new TestService();
        s.setInjector(injector);
        s.setConfig(config);
        s.setSession(session);
        s.setSearchFactory(factory);
        s.setFullTextSession(ftSession);
        s.setSecurityContext(c);
        s.setUriInfo(info);

        assertEquals(injector, s.getInjector());
        assertEquals(session, s.getSession());
        assertEquals(factory, s.getSearchFactory());
        assertEquals(ftSession, s.getFullTextSession());
        assertEquals(c, s.getSecurityContext());
        assertEquals(config, s.getConfig());
        assertEquals(info, s.getUriInfo());
    }

    /**
     * Assert that we can get the current user principal.
     */
    @Test
    public void testGetCurrentUser() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .bearerToken()
                .build();

        Mockito.doReturn(getAdminContext().getToken())
                .when(mockContext).getUserPrincipal();

        User user = service.getCurrentUser();
        assertNotNull(user);
        assertEquals(adminContext.getToken().getIdentity().getUser(),
                user);
    }

    /**
     * Assert that a token issued to a client credential user does not return
     * a user.
     */
    @Test
    public void testGetCurrentUserClientCredentials() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.ClientCredentials)
                .bearerToken()
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();

        assertNull(service.getCurrentUser());
    }

    /**
     * Assert that we can get the current admin application.
     */
    @Test
    public void testGetAdminApplication() {
        Application a = service.getAdminApplication();
        assertEquals(getAdminContext().getApplication(), a);
    }

    /**
     * Assert that a nonexistent entity will show up as not found.
     */
    @Test(expected = NotFoundException.class)
    public void testAssertCanAccessNull() {
        service.assertCanAccess(null, Scope.APPLICATION_ADMIN);
    }

    /**
     * Assert that a current owner of an entity can access it.
     */
    @Test
    public void testAssertCanAccessAsOwner() {
        // Add a new, non-admin user, and make it the owner of the user app
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .role("lol")
                .user()
                .identity("not_admin")
                .bearerToken()
                .build();
        ApplicationContext userContext = userApp.getBuilder()
                .owner(adminContext.getUser())
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();

        service.assertCanAccess(userContext.getApplication(),
                Scope.APPLICATION_ADMIN);
    }

    /**
     * Assert that a non owner of an entity can access it if they have the
     * correct scope.
     */
    @Test
    public void testAssertCanAccessNotOwnerValidScope() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .bearerToken(Scope.APPLICATION_ADMIN)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userApp.getApplication(),
                Scope.APPLICATION_ADMIN);
    }

    /**
     * Assert that a non owner of an entity cannot access it if they have the
     * incorrect scope.
     */
    @Test(expected = NotFoundException.class)
    public void testAssertCanAccessNotOwnerInvalidScope() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .bearerToken(Scope.APPLICATION)
                .build();

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
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.ClientCredentials)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION_ADMIN)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.assertCanAccess(userApp.getApplication(),
                Scope.APPLICATION_ADMIN);
        assertTrue(true);
    }

    /**
     * Assert that a client credentials token cannot access the entity if they
     * do not have the admin scope.
     */
    @Test(expected = NotFoundException.class)
    public void testAssertCanAccessClientCredentialsInvalidScope() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.ClientCredentials)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

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
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        User user = service.resolveOwnershipFilter(null);
        assertNull(user);
    }

    /**
     * Assert that an admin, when passing an invalid ownerID, gets an error.
     */
    @Test(expected = BadRequestException.class)
    public void testRequestInvalidUserFilterAdminFilter() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .user()
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.resolveOwnershipFilter(IdUtil.next());
    }

    /**
     * Assert that an admin, when passing an ownerID, gets the owner ID.
     */
    @Test
    public void testRequestUserFilterAdminFilter() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .user()
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        User requestedUser = adminContext.getUser();
        User user = service.resolveOwnershipFilter(requestedUser.getId());
        assertEquals(requestedUser, user);
    }

    /**
     * Assert that a non admin defaults to self-filtering.
     */
    @Test
    public void testRequestUserFilterNonAdminNoFilter() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        User requestedUser = adminContext.getUser();
        User user = service.resolveOwnershipFilter(null);
        assertEquals(adminContext.getUser(), user);
    }

    /**
     * Assert that a non admin cannot filter by a different user.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRequestUserFilterNonAdminFilter() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .user()
                .build();

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
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        User requestedUser = adminContext.getToken().getIdentity().getUser();
        User user = service.resolveOwnershipFilter(requestedUser.getId());
        assertEquals(requestedUser, user);
    }

    /**
     * Assert that a client credentials client cannot filter without the
     * appropriate scope.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRequestUserFilterNonAdminClientCredentials() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.ClientCredentials)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveOwnershipFilter(adminContext.getUser().getId());
    }

    /**
     * Assert that we can require a null entity.
     */
    @Test(expected = BadRequestException.class)
    public void testRequireEntityInputNullEntity() {
        service.requireEntityInput(Application.class, null);
    }

    /**
     * Assert that we can require an entity with a null ID.
     */
    @Test(expected = BadRequestException.class)
    public void testRequireEntityInputNoIdEntity() {
        service.requireEntityInput(Application.class, new Application());
    }

    /**
     * Assert that we can require a valid entity.
     */
    @Test
    public void testRequireValidEntity() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        Application a =
                service.requireEntityInput(Application.class,
                        userApp.getApplication());
        assertEquals(userApp.getApplication(), a);
    }

    /**
     * Assert that passing no entity ID as an admin returns null.
     */
    @Test
    public void testResolveFilterEntityNoEntityAdmin() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        Application result =
                service.resolveFilterEntity(Application.class, null);
        assertNull(result);
    }

    /**
     * Assert that passing a nonexitent entity ID as an admin throws a 404.
     */
    @Test(expected = BadRequestException.class)
    public void testResolveFilterEntityNonexistentEntityIdAdmin() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        service.resolveFilterEntity(Application.class, IdUtil.next());
    }

    /**
     * Assert that passing a valid entity ID as an admin, we get the entity.
     */
    @Test
    public void testResolveFilterEntityValidEntityIdAdmin() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION_ADMIN);

        Application a =
                service.resolveFilterEntity(Application.class,
                        userApp.getApplication().getId());
        assertEquals(userApp.getApplication(), a);
    }

    /**
     * Assert that an invalidly scoped token throws an exception.
     */
    @Test(expected = InvalidScopeException.class)
    public void testResolveFilterEntityInvalidScope() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

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
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        Application a = service.resolveFilterEntity(Application.class, null);
        assertNull(a);
    }

    /**
     * Assert that passing a nonexitent entity ID as a regular user throws an
     * invalidScope exception.
     */
    @Test(expected = BadRequestException.class)
    public void testResolveFilterEntityNonexistentEntityIdNonAdmin() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.Implicit)
                .authenticator(AuthenticatorType.Test)
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveFilterEntity(Application.class, IdUtil.next());
    }

    /**
     * Assert that trying to filter on an entity with a token that has no
     * user identity and a regular scope throws an exception.
     */
    @Test(expected = InvalidScopeException.class)
    public void testResolveFilterEntityNoTokenIdentity() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .client(ClientType.ClientCredentials)
                .bearerToken(Scope.APPLICATION)
                .build();

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
    @Test(expected = BadRequestException.class)
    public void testResolveFilterEntityNonAdminNonOwner() {
        // Set up an owner for the user app.
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .user().identity()
                .build();
        ApplicationContext userContext = userApp.getBuilder()
                .owner(adminContext.getUser())
                .build();
        adminContext = adminContext.getBuilder()
                .user()
                .identity()
                .bearerToken(Scope.APPLICATION)
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        service.resolveFilterEntity(Application.class,
                userContext.getApplication().getId());
    }

    /**
     * Assert that a regular user can filter on an entity if they are the owner.
     */
    @Test
    public void testResolveFilterEntityNonAdminOwner() {
        ApplicationContext adminContext = getAdminContext().getBuilder()
                .user().identity()
                .bearerToken(Scope.APPLICATION)
                .build();
        ApplicationContext userContext = userApp.getBuilder()
                .owner(adminContext.getUser())
                .build();

        Mockito.doReturn(adminContext.getToken())
                .when(mockContext).getUserPrincipal();
        Mockito.doReturn(true).when(mockContext)
                .isUserInRole(Scope.APPLICATION);

        Application a =
                service.resolveFilterEntity(Application.class,
                        userContext.getApplication().getId());
        assertEquals(adminContext.getUser(), a.getOwner());
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
