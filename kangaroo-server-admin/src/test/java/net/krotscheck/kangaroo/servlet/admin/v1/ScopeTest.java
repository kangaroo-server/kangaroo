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

package net.krotscheck.kangaroo.servlet.admin.v1;

import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

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
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert the expected constant values.
     */
    @Test
    public void testExpectedConstants() {
        Assert.assertEquals("kangaroo:application",
                Scope.APPLICATION);
        Assert.assertEquals("kangaroo:application_admin",
                Scope.APPLICATION_ADMIN);
        Assert.assertEquals("kangaroo:authenticator",
                Scope.AUTHENTICATOR);
        Assert.assertEquals("kangaroo:authenticator_admin",
                Scope.AUTHENTICATOR_ADMIN);
        Assert.assertEquals("kangaroo:client",
                Scope.CLIENT);
        Assert.assertEquals("kangaroo:client_admin",
                Scope.CLIENT_ADMIN);
        Assert.assertEquals("kangaroo:user",
                Scope.USER);
        Assert.assertEquals("kangaroo:user_admin",
                Scope.USER_ADMIN);
        Assert.assertEquals("kangaroo:role",
                Scope.ROLE);
        Assert.assertEquals("kangaroo:role_admin",
                Scope.ROLE_ADMIN);
        Assert.assertEquals("kangaroo:identity",
                Scope.IDENTITY);
        Assert.assertEquals("kangaroo:identity_admin",
                Scope.IDENTITY_ADMIN);
        Assert.assertEquals("kangaroo:scope",
                Scope.SCOPE);
        Assert.assertEquals("kangaroo:scope_admin",
                Scope.SCOPE_ADMIN);
        Assert.assertEquals("kangaroo:token",
                Scope.TOKEN);
        Assert.assertEquals("kangaroo:token_admin",
                Scope.TOKEN_ADMIN);
    }

    /**
     * Test that the list of scopes is all there.
     */
    @Test
    public void testAllScopeList() {
        List<String> allScopes = Scope.allScopes();

        Assert.assertTrue(allScopes.contains(Scope.APPLICATION));
        Assert.assertTrue(allScopes.contains(Scope.APPLICATION_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.AUTHENTICATOR));
        Assert.assertTrue(allScopes.contains(Scope.AUTHENTICATOR_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.CLIENT));
        Assert.assertTrue(allScopes.contains(Scope.CLIENT_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.USER));
        Assert.assertTrue(allScopes.contains(Scope.USER_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.ROLE));
        Assert.assertTrue(allScopes.contains(Scope.ROLE_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.IDENTITY));
        Assert.assertTrue(allScopes.contains(Scope.IDENTITY_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.SCOPE));
        Assert.assertTrue(allScopes.contains(Scope.SCOPE_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.TOKEN));
        Assert.assertTrue(allScopes.contains(Scope.TOKEN_ADMIN));

        Assert.assertEquals(16, allScopes.size());
    }

    /**
     * Test that the list of admin scopes is all there.
     */
    @Test
    public void testAdminScopeList() {
        List<String> allScopes = Scope.adminScopes();

        Assert.assertFalse(allScopes.contains(Scope.APPLICATION));
        Assert.assertTrue(allScopes.contains(Scope.APPLICATION_ADMIN));
        Assert.assertFalse(allScopes.contains(Scope.AUTHENTICATOR));
        Assert.assertTrue(allScopes.contains(Scope.AUTHENTICATOR_ADMIN));
        Assert.assertFalse(allScopes.contains(Scope.CLIENT));
        Assert.assertTrue(allScopes.contains(Scope.CLIENT_ADMIN));
        Assert.assertFalse(allScopes.contains(Scope.USER));
        Assert.assertTrue(allScopes.contains(Scope.USER_ADMIN));
        Assert.assertFalse(allScopes.contains(Scope.ROLE));
        Assert.assertTrue(allScopes.contains(Scope.ROLE_ADMIN));
        Assert.assertFalse(allScopes.contains(Scope.IDENTITY));
        Assert.assertTrue(allScopes.contains(Scope.IDENTITY_ADMIN));
        Assert.assertFalse(allScopes.contains(Scope.SCOPE));
        Assert.assertTrue(allScopes.contains(Scope.SCOPE_ADMIN));
        Assert.assertFalse(allScopes.contains(Scope.TOKEN));
        Assert.assertTrue(allScopes.contains(Scope.TOKEN_ADMIN));

        Assert.assertEquals(8, allScopes.size());
    }

    /**
     * Test that the list of admin scopes is all there.
     */
    @Test
    public void testUserScopeList() {
        List<String> allScopes = Scope.userScopes();

        Assert.assertTrue(allScopes.contains(Scope.APPLICATION));
        Assert.assertFalse(allScopes.contains(Scope.APPLICATION_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.AUTHENTICATOR));
        Assert.assertFalse(allScopes.contains(Scope.AUTHENTICATOR_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.CLIENT));
        Assert.assertFalse(allScopes.contains(Scope.CLIENT_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.USER));
        Assert.assertFalse(allScopes.contains(Scope.USER_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.ROLE));
        Assert.assertFalse(allScopes.contains(Scope.ROLE_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.IDENTITY));
        Assert.assertFalse(allScopes.contains(Scope.IDENTITY_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.SCOPE));
        Assert.assertFalse(allScopes.contains(Scope.SCOPE_ADMIN));
        Assert.assertTrue(allScopes.contains(Scope.TOKEN));
        Assert.assertFalse(allScopes.contains(Scope.TOKEN_ADMIN));

        Assert.assertEquals(8, allScopes.size());
    }

    /**
     * Test all the cases for retrieving appropriate scope names for entities.
     */
    @Test
    public void testGetScopeForEntity() {
        Assert.assertEquals(Scope.APPLICATION,
                Scope.forEntity(new Application(), false));
        Assert.assertEquals(Scope.APPLICATION_ADMIN,
                Scope.forEntity(new Application(), true));
        Assert.assertEquals(Scope.AUTHENTICATOR,
                Scope.forEntity(new Authenticator(), false));
        Assert.assertEquals(Scope.AUTHENTICATOR_ADMIN,
                Scope.forEntity(new Authenticator(), true));
        Assert.assertEquals(Scope.CLIENT,
                Scope.forEntity(new Client(), false));
        Assert.assertEquals(Scope.CLIENT_ADMIN,
                Scope.forEntity(new Client(), true));
        Assert.assertEquals(Scope.USER,
                Scope.forEntity(new User(), false));
        Assert.assertEquals(Scope.USER_ADMIN,
                Scope.forEntity(new User(), true));
        Assert.assertEquals(Scope.ROLE,
                Scope.forEntity(new Role(), false));
        Assert.assertEquals(Scope.ROLE_ADMIN,
                Scope.forEntity(new Role(), true));
        Assert.assertEquals(Scope.IDENTITY,
                Scope.forEntity(new UserIdentity(), false));
        Assert.assertEquals(Scope.IDENTITY_ADMIN,
                Scope.forEntity(new UserIdentity(), true));
        Assert.assertEquals(Scope.SCOPE,
                Scope.forEntity(new ApplicationScope(), false));
        Assert.assertEquals(Scope.SCOPE_ADMIN,
                Scope.forEntity(new ApplicationScope(), true));
        Assert.assertEquals(Scope.TOKEN,
                Scope.forEntity(new OAuthToken(), false));
        Assert.assertEquals(Scope.TOKEN_ADMIN,
                Scope.forEntity(new OAuthToken(), true));
        Assert.assertEquals("kangaroo:unknown",
                Scope.forEntity(null, true));
    }
}
