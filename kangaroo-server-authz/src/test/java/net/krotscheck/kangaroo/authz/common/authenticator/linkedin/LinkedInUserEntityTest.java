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

package net.krotscheck.kangaroo.authz.common.authenticator.linkedin;

import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the linkedin user entity PoJo. Not all available properties
 * are serialized, only the ones that matter right now.
 *
 * @author Michael Krotscheck
 */
public final class LinkedInUserEntityTest {

    /**
     * Assert we can get/set the first name.
     */
    @Test
    public void getSetFirstName() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        LinkedInUserEntity token = new LinkedInUserEntity();

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
        LinkedInUserEntity token = new LinkedInUserEntity();

        assertNull(token.getLastName());
        token.setLastName(randomValue);
        assertEquals(randomValue, token.getLastName());
    }

    /**
     * Assert we can get/set the id.
     */
    @Test
    public void getSetId() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        LinkedInUserEntity token = new LinkedInUserEntity();

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
        LinkedInUserEntity token = new LinkedInUserEntity();

        assertNull(token.getEmailAddress());
        token.setEmailAddress(randomValue);
        assertEquals(randomValue, token.getEmailAddress());
    }

    /**
     * Assert that we can convert an entity to claims.
     */
    @Test
    public void testAsGenericUser() {
        LinkedInUserEntity token = new LinkedInUserEntity();

        token.setId(RandomStringUtils.randomAlphabetic(30));
        token.setFirstName(RandomStringUtils.randomAlphabetic(30));
        token.setLastName(RandomStringUtils.randomAlphabetic(30));
        token.setEmailAddress(RandomStringUtils.randomAlphabetic(30));

        OAuth2User user = token.asGenericUser();

        assertEquals(token.getId(),
                user.getId());
        assertEquals(token.getFirstName(),
                user.getClaims().get("firstName"));
        assertEquals(token.getLastName(),
                user.getClaims().get("lastName"));
        assertEquals(token.getEmailAddress(),
                user.getClaims().get("emailAddress"));
    }
}
