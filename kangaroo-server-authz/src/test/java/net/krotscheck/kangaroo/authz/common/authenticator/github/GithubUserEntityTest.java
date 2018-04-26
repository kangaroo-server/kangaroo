/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.common.authenticator.github;

import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the github user entity.
 *
 * @author Michael Krotscheck
 */
public final class GithubUserEntityTest {

    /**
     * Assert we can get/set the name.
     */
    @Test
    public void getSetName() {
        String randomValue = randomAlphabetic(30);
        GithubUserEntity token = new GithubUserEntity();

        assertNull(token.getName());
        token.setName(randomValue);
        assertEquals(randomValue, token.getName());
    }

    /**
     * Assert we can get/set the id.
     */
    @Test
    public void getSetId() {
        Integer randomValue = RandomUtils.nextInt();
        GithubUserEntity token = new GithubUserEntity();

        assertNull(token.getId());
        token.setId(randomValue);
        assertEquals(randomValue, token.getId());
    }

    /**
     * Assert we can get/set the email.
     */
    @Test
    public void getSetEmail() {
        String randomValue = randomAlphabetic(30);
        GithubUserEntity token = new GithubUserEntity();

        assertNull(token.getEmail());
        token.setEmail(randomValue);
        assertEquals(randomValue, token.getEmail());
    }

    /**
     * Assert we can get/set the login.
     */
    @Test
    public void getSetLogin() {
        String randomValue = randomAlphabetic(30);
        GithubUserEntity token = new GithubUserEntity();

        assertNull(token.getLogin());
        token.setLogin(randomValue);
        assertEquals(randomValue, token.getLogin());
    }

    /**
     * Assert that we can convert an entity to claims.
     */
    @Test
    public void testAsGenericUser() {
        GithubUserEntity token = new GithubUserEntity();

        token.setId(RandomUtils.nextInt());
        token.setName(randomAlphabetic(30));
        token.setLogin(randomAlphabetic(30));
        token.setEmail(randomAlphabetic(30));

        OAuth2User user = token.asGenericUser();

        assertEquals(token.getId().toString(), user.getId());
        assertEquals(token.getName(), user.getClaims().get("name"));
        assertEquals(token.getLogin(), user.getClaims().get("login"));
        assertEquals(token.getEmail(), user.getClaims().get("email"));
    }
}
