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

package net.krotscheck.kangaroo.authz.common.authenticator.oauth2;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * Unit test for the pojo used to map the authtoken to that we get from
 * an OAuth2 provider.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2IdPTokenTest {

    /**
     * Test get/set of the access token.
     */
    @Test
    public void getSetAccessToken() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        OAuth2IdPToken token = new OAuth2IdPToken();

        assertNull(token.getAccessToken());
        token.setAccessToken(randomValue);
        assertEquals(randomValue, token.getAccessToken());
    }

    /**
     * Test get/set of the token type.
     */
    @Test
    public void getSetTokenType() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        OAuth2IdPToken token = new OAuth2IdPToken();

        assertNull(token.getTokenType());
        token.setTokenType(randomValue);
        assertEquals(randomValue, token.getTokenType());
    }

    /**
     * Test get/set of the expires_in field.
     */
    @Test
    public void getSetExpiresIn() {
        OAuth2IdPToken token = new OAuth2IdPToken();

        assertNull(token.getExpiresIn());
        token.setExpiresIn(Long.valueOf(100));
        assertEquals(Long.valueOf(100), token.getExpiresIn());
    }

    /**
     * Test get/set of the refresh token (where applicable).
     */
    @Test
    public void getSetRefreshToken() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        OAuth2IdPToken token = new OAuth2IdPToken();

        assertNull(token.getRefreshToken());
        token.setRefreshToken(randomValue);
        assertEquals(randomValue, token.getRefreshToken());
    }

    /**
     * Test get/set of the scopes.
     */
    @Test
    public void getSetScope() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        OAuth2IdPToken token = new OAuth2IdPToken();

        assertNull(token.getScope());
        token.setScope(randomValue);
        assertEquals(randomValue, token.getScope());
    }

    /**
     * Test get/set of the state.
     */
    @Test
    public void getSetState() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        OAuth2IdPToken token = new OAuth2IdPToken();

        assertNull(token.getState());
        token.setState(randomValue);
        assertEquals(randomValue, token.getState());
    }
}
