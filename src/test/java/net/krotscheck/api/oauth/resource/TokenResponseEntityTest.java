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

package net.krotscheck.api.oauth.resource;

import net.krotscheck.features.database.entity.ApplicationScope;
import net.krotscheck.features.database.entity.OAuthToken;
import net.krotscheck.features.database.entity.OAuthTokenType;
import org.junit.Assert;
import org.junit.Test;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

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
        token.setId(UUID.randomUUID());
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

        TokenResponseEntity entity = TokenResponseEntity.factory(token);
        Assert.assertEquals(token.getId(), entity.getAccessToken());
        Assert.assertEquals(token.getExpiresIn(),
                (long) entity.getExpiresIn());
        Assert.assertEquals("Bearer", entity.getTokenType().toString());
        Assert.assertEquals("debug test", entity.getScope());
        Assert.assertNull(entity.getRefreshToken());
    }

    /**
     * Assert that we can create a token with a refresh.
     */
    @Test
    public void testFactoryRefresh() {
        OAuthToken token = new OAuthToken();
        token.setId(UUID.randomUUID());
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
        refresh.setId(UUID.randomUUID());
        refresh.setTokenType(OAuthTokenType.Refresh);

        TokenResponseEntity entity = TokenResponseEntity.factory(token,
                refresh);
        Assert.assertEquals(token.getId(), entity.getAccessToken());
        Assert.assertEquals(token.getExpiresIn(),
                (long) entity.getExpiresIn());
        Assert.assertEquals("Bearer", entity.getTokenType().toString());
        Assert.assertEquals("debug test", entity.getScope());
        Assert.assertEquals(refresh.getId(), entity.getRefreshToken());
    }
}
