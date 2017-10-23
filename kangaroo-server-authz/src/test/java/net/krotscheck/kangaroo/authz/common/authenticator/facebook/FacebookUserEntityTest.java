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

package net.krotscheck.kangaroo.authz.common.authenticator.facebook;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the facebook user entity PoJo. Not all available properties
 * are serialized, only the ones that matter right now.
 *
 * @author Michael Krotscheck
 */
public final class FacebookUserEntityTest {

    /**
     * Assert we can get/set the name.
     */
    @Test
    public void getSetName() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        FacebookUserEntity token = new FacebookUserEntity();

        assertNull(token.getName());
        token.setName(randomValue);
        assertEquals(randomValue, token.getName());
    }

    /**
     * Assert we can get/set the id.
     */
    @Test
    public void getSetId() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        FacebookUserEntity token = new FacebookUserEntity();

        assertNull(token.getId());
        token.setId(randomValue);
        assertEquals(randomValue, token.getId());
    }

    /**
     * Assert we can get/set the email.
     */
    @Test
    public void getSetEmail() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        FacebookUserEntity token = new FacebookUserEntity();

        assertNull(token.getEmail());
        token.setEmail(randomValue);
        assertEquals(randomValue, token.getEmail());
    }

    /**
     * Assert we can get/set the first name.
     */
    @Test
    public void getSetFirstName() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        FacebookUserEntity token = new FacebookUserEntity();

        assertNull(token.getFirstName());
        token.setFirstName(randomValue);
        assertEquals(randomValue, token.getFirstName());
    }

    /**
     * Assert we can get/set the last name.
     */
    @Test
    public void getSetLastName() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        FacebookUserEntity token = new FacebookUserEntity();

        assertNull(token.getLastName());
        token.setLastName(randomValue);
        assertEquals(randomValue, token.getLastName());
    }

    /**
     * Assert we can get/set the middle name.
     */
    @Test
    public void getSetMiddleName() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        FacebookUserEntity token = new FacebookUserEntity();

        assertNull(token.getMiddleName());
        token.setMiddleName(randomValue);
        assertEquals(randomValue, token.getMiddleName());
    }

    /**
     * Assert that we can convert an entity to claims.
     */
    @Test
    public void toClaims() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        FacebookUserEntity token = new FacebookUserEntity();

        token.setId(RandomStringUtils.randomAlphabetic(30));
        token.setName(RandomStringUtils.randomAlphabetic(30));
        token.setFirstName(RandomStringUtils.randomAlphabetic(30));
        token.setLastName(RandomStringUtils.randomAlphabetic(30));
        token.setMiddleName(RandomStringUtils.randomAlphabetic(30));

        Map<String, String> claims = token.toClaims();

        assertEquals(token.getName(), claims.get("name"));
        assertEquals(token.getFirstName(), claims.get("firstName"));
        assertEquals(token.getMiddleName(), claims.get("middleName"));
        assertEquals(token.getLastName(), claims.get("lastName"));
        assertEquals(token.getEmail(), claims.get("email"));
    }
}
