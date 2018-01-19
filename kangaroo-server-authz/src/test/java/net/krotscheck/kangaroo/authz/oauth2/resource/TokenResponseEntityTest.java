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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.junit.Test;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * Unit tests for our response entity factory.
 */
public final class TokenResponseEntityTest {

    /**
     * Assert that we can create a token with no refresh token.
     */
    @Test
    public void testFactoryNoRefresh() {
        OAuthToken token = new OAuthToken();
        token.setId(IdUtil.next());
        token.setTokenType(OAuthTokenType.Bearer);
        token.setExpiresIn(100);

        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        ApplicationScope debug = new ApplicationScope();
        debug.setName("debug");
        ApplicationScope test = new ApplicationScope();
        test.setName("test");
        scopes.put("debug", debug);
        scopes.put("test", test);
        token.setScopes(scopes);

        String state = IdUtil.toString(IdUtil.next());

        TokenResponseEntity entity = TokenResponseEntity.factory(token, state);
        assertEquals(token.getId(), entity.getAccessToken());
        assertEquals(token.getExpiresIn().longValue(),
                (long) entity.getExpiresIn());
        assertEquals("Bearer", entity.getTokenType().toString());
        assertEquals("debug test", entity.getScope());
        assertNull(entity.getRefreshToken());
        assertEquals(state, entity.getState());
    }

    /**
     * Assert that we can create a token with a refresh.
     */
    @Test
    public void testFactoryRefresh() {
        OAuthToken token = new OAuthToken();
        token.setId(IdUtil.next());
        token.setTokenType(OAuthTokenType.Bearer);
        token.setExpiresIn(100);

        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        ApplicationScope debug = new ApplicationScope();
        debug.setName("debug");
        ApplicationScope test = new ApplicationScope();
        test.setName("test");
        scopes.put("debug", debug);
        scopes.put("test", test);
        token.setScopes(scopes);

        OAuthToken refresh = new OAuthToken();
        refresh.setId(IdUtil.next());
        refresh.setTokenType(OAuthTokenType.Refresh);

        String state = IdUtil.toString(IdUtil.next());

        TokenResponseEntity entity = TokenResponseEntity.factory(token,
                refresh, state);
        assertEquals(token.getId(), entity.getAccessToken());
        assertEquals(token.getExpiresIn().longValue(),
                (long) entity.getExpiresIn());
        assertEquals("Bearer", entity.getTokenType().toString());
        assertEquals("debug test", entity.getScope());
        assertEquals(refresh.getId(), entity.getRefreshToken());
        assertEquals(state, entity.getState());
    }

    /**
     * Assert that a token with no scope sets the scope string to null.
     */
    @Test
    public void testFactoryNoScope() {
        OAuthToken token = new OAuthToken();
        token.setId(IdUtil.next());
        token.setTokenType(OAuthTokenType.Bearer);
        token.setExpiresIn(100);

        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        token.setScopes(scopes);

        OAuthToken refresh = new OAuthToken();
        refresh.setId(IdUtil.next());
        refresh.setTokenType(OAuthTokenType.Refresh);

        String state = IdUtil.toString(IdUtil.next());

        TokenResponseEntity entity = TokenResponseEntity.factory(token,
                refresh, state);
        assertEquals(token.getId(), entity.getAccessToken());
        assertEquals(token.getExpiresIn().longValue(),
                (long) entity.getExpiresIn());
        assertEquals("Bearer", entity.getTokenType().toString());
        assertNull(entity.getScope());
        assertEquals(refresh.getId(), entity.getRefreshToken());
        assertEquals(state, entity.getState());
    }
}
