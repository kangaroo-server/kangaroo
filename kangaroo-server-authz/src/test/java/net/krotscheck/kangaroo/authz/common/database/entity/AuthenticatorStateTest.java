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

package net.krotscheck.kangaroo.authz.common.database.entity;

import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for the AuthenticatorState entity.
 *
 * @author Michael Krotscheck
 */
public final class AuthenticatorStateTest {

    /**
     * Assert that we can get and set the authenticator.
     */
    @Test
    public void testGetSetAuthenticator() {
        AuthenticatorState token = new AuthenticatorState();
        Authenticator authenticator = new Authenticator();

        assertNull(token.getAuthenticator());
        token.setAuthenticator(authenticator);
        assertEquals(authenticator, token.getAuthenticator());
    }

    /**
     * Assert that we can get and set client state.
     */
    @Test
    public void testGetSetClientState() {
        AuthenticatorState state = new AuthenticatorState();

        // Default
        assertNull(state.getClientState());
        state.setClientState("state");
        assertEquals("state", state.getClientState());
    }

    /**
     * Assert that we can get and set client redirect.
     */
    @Test
    public void testGetSetClientRedirect() {
        AuthenticatorState state = new AuthenticatorState();
        URI testUri = UriBuilder.fromUri("http://valid.example.com/").build();

        // Default
        assertNull(state.getClientRedirect());
        state.setClientRedirect(testUri);
        assertEquals(testUri, state.getClientRedirect());
    }

    /**
     * Test get/set scope list.
     */
    @Test
    public void testGetSetScopes() {
        AuthenticatorState state = new AuthenticatorState();
        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        scopes.put("test", new ApplicationScope());

        assertEquals(0, state.getClientScopes().size());
        state.setClientScopes(scopes);
        assertEquals(scopes, state.getClientScopes());
        assertNotSame(scopes, state.getClientScopes());
    }

    /**
     * Assert that we retrieve the owner from the parent authenticator.
     */
    @Test
    public void testGetOwner() {
        AuthenticatorState state = new AuthenticatorState();
        Authenticator spy = spy(new Authenticator());

        // Null check
        assertNull(state.getOwner());

        state.setAuthenticator(spy);
        state.getOwner();

        Mockito.verify(spy).getOwner();
    }
}
