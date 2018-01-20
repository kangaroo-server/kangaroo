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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the generic user type.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2UserTest {

    /**
     * Test get/set the ID.
     */
    @Test
    public void getSetId() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        OAuth2User user = new OAuth2User();

        assertNull(user.getId());
        user.setId(randomValue);
        assertEquals(randomValue, user.getId());
    }

    /**
     * Test get/set the claims.
     */
    @Test
    public void getSetClaims() {
        OAuth2User user = new OAuth2User();

        assertNotNull(user.getClaims());
        assertEquals(0, user.getClaims().size());

        Map<String, String> claims = new HashMap<>();
        claims.put("foo", "bar");

        user.setClaims(claims);
        assertEquals(claims, user.getClaims());
        assertNotSame(claims, user.getClaims());
    }
}
