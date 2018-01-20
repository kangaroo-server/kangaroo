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

package net.krotscheck.kangaroo.authz.admin;

import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for our Scope constants.
 *
 * @author Michael Krotscheck
 */
public final class ScopeTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = Scope.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert the expected constant values.
     */
    @Test
    public void testExpectedConstants() {
        assertEquals("kangaroo:application",
                Scope.APPLICATION);
        assertEquals("kangaroo:application_admin",
                Scope.APPLICATION_ADMIN);
        assertEquals("kangaroo:authenticator",
                Scope.AUTHENTICATOR);
        assertEquals("kangaroo:authenticator_admin",
                Scope.AUTHENTICATOR_ADMIN);
        assertEquals("kangaroo:client",
                Scope.CLIENT);
        assertEquals("kangaroo:client_admin",
                Scope.CLIENT_ADMIN);
        assertEquals("kangaroo:user",
                Scope.USER);
        assertEquals("kangaroo:user_admin",
                Scope.USER_ADMIN);
        assertEquals("kangaroo:role",
                Scope.ROLE);
        assertEquals("kangaroo:role_admin",
                Scope.ROLE_ADMIN);
        assertEquals("kangaroo:identity",
                Scope.IDENTITY);
        assertEquals("kangaroo:identity_admin",
                Scope.IDENTITY_ADMIN);
        assertEquals("kangaroo:scope",
                Scope.SCOPE);
        assertEquals("kangaroo:scope_admin",
                Scope.SCOPE_ADMIN);
        assertEquals("kangaroo:token",
                Scope.TOKEN);
        assertEquals("kangaroo:token_admin",
                Scope.TOKEN_ADMIN);
    }

    /**
     * Test that the list of scopes is all there.
     */
    @Test
    public void testAllScopeList() {
        List<String> allScopes = Scope.allScopes();

        assertTrue(allScopes.contains(Scope.APPLICATION));
        assertTrue(allScopes.contains(Scope.APPLICATION_ADMIN));
        assertTrue(allScopes.contains(Scope.AUTHENTICATOR));
        assertTrue(allScopes.contains(Scope.AUTHENTICATOR_ADMIN));
        assertTrue(allScopes.contains(Scope.CLIENT));
        assertTrue(allScopes.contains(Scope.CLIENT_ADMIN));
        assertTrue(allScopes.contains(Scope.USER));
        assertTrue(allScopes.contains(Scope.USER_ADMIN));
        assertTrue(allScopes.contains(Scope.ROLE));
        assertTrue(allScopes.contains(Scope.ROLE_ADMIN));
        assertTrue(allScopes.contains(Scope.IDENTITY));
        assertTrue(allScopes.contains(Scope.IDENTITY_ADMIN));
        assertTrue(allScopes.contains(Scope.SCOPE));
        assertTrue(allScopes.contains(Scope.SCOPE_ADMIN));
        assertTrue(allScopes.contains(Scope.TOKEN));
        assertTrue(allScopes.contains(Scope.TOKEN_ADMIN));

        assertEquals(16, allScopes.size());
    }

    /**
     * Test that the list of admin scopes is all there.
     */
    @Test
    public void testAdminScopeList() {
        List<String> allScopes = Scope.adminScopes();

        assertFalse(allScopes.contains(Scope.APPLICATION));
        assertTrue(allScopes.contains(Scope.APPLICATION_ADMIN));
        assertFalse(allScopes.contains(Scope.AUTHENTICATOR));
        assertTrue(allScopes.contains(Scope.AUTHENTICATOR_ADMIN));
        assertFalse(allScopes.contains(Scope.CLIENT));
        assertTrue(allScopes.contains(Scope.CLIENT_ADMIN));
        assertFalse(allScopes.contains(Scope.USER));
        assertTrue(allScopes.contains(Scope.USER_ADMIN));
        assertFalse(allScopes.contains(Scope.ROLE));
        assertTrue(allScopes.contains(Scope.ROLE_ADMIN));
        assertFalse(allScopes.contains(Scope.IDENTITY));
        assertTrue(allScopes.contains(Scope.IDENTITY_ADMIN));
        assertFalse(allScopes.contains(Scope.SCOPE));
        assertTrue(allScopes.contains(Scope.SCOPE_ADMIN));
        assertFalse(allScopes.contains(Scope.TOKEN));
        assertTrue(allScopes.contains(Scope.TOKEN_ADMIN));

        assertEquals(8, allScopes.size());
    }

    /**
     * Test that the list of admin scopes is all there.
     */
    @Test
    public void testUserScopeList() {
        List<String> allScopes = Scope.userScopes();

        assertTrue(allScopes.contains(Scope.APPLICATION));
        assertFalse(allScopes.contains(Scope.APPLICATION_ADMIN));
        assertTrue(allScopes.contains(Scope.AUTHENTICATOR));
        assertFalse(allScopes.contains(Scope.AUTHENTICATOR_ADMIN));
        assertTrue(allScopes.contains(Scope.CLIENT));
        assertFalse(allScopes.contains(Scope.CLIENT_ADMIN));
        assertTrue(allScopes.contains(Scope.USER));
        assertFalse(allScopes.contains(Scope.USER_ADMIN));
        assertTrue(allScopes.contains(Scope.ROLE));
        assertFalse(allScopes.contains(Scope.ROLE_ADMIN));
        assertTrue(allScopes.contains(Scope.IDENTITY));
        assertFalse(allScopes.contains(Scope.IDENTITY_ADMIN));
        assertTrue(allScopes.contains(Scope.SCOPE));
        assertFalse(allScopes.contains(Scope.SCOPE_ADMIN));
        assertTrue(allScopes.contains(Scope.TOKEN));
        assertFalse(allScopes.contains(Scope.TOKEN_ADMIN));

        assertEquals(8, allScopes.size());
    }

    /**
     * Test all the cases for retrieving appropriate scope names for entities.
     */
    @Test
    public void testGetScopeForEntity() {
        assertEquals(Scope.APPLICATION,
                Scope.forEntity(new Application(), false));
        assertEquals(Scope.APPLICATION_ADMIN,
                Scope.forEntity(new Application(), true));
        assertEquals(Scope.AUTHENTICATOR,
                Scope.forEntity(new Authenticator(), false));
        assertEquals(Scope.AUTHENTICATOR_ADMIN,
                Scope.forEntity(new Authenticator(), true));
        assertEquals(Scope.CLIENT,
                Scope.forEntity(new Client(), false));
        assertEquals(Scope.CLIENT_ADMIN,
                Scope.forEntity(new Client(), true));
        assertEquals(Scope.USER,
                Scope.forEntity(new User(), false));
        assertEquals(Scope.USER_ADMIN,
                Scope.forEntity(new User(), true));
        assertEquals(Scope.ROLE,
                Scope.forEntity(new Role(), false));
        assertEquals(Scope.ROLE_ADMIN,
                Scope.forEntity(new Role(), true));
        assertEquals(Scope.IDENTITY,
                Scope.forEntity(new UserIdentity(), false));
        assertEquals(Scope.IDENTITY_ADMIN,
                Scope.forEntity(new UserIdentity(), true));
        assertEquals(Scope.SCOPE,
                Scope.forEntity(new ApplicationScope(), false));
        assertEquals(Scope.SCOPE_ADMIN,
                Scope.forEntity(new ApplicationScope(), true));
        assertEquals(Scope.TOKEN,
                Scope.forEntity(new OAuthToken(), false));
        assertEquals(Scope.TOKEN_ADMIN,
                Scope.forEntity(new OAuthToken(), true));
        assertEquals("kangaroo:unknown",
                Scope.forEntity(null, true));
    }
}
