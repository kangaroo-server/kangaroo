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

package net.krotscheck.kangaroo.authz.common.authenticator.google;

import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the google user entity PoJo. Not all available properties
 * are serialized, only the ones that matter right now.
 *
 * @author Michael Krotscheck
 */
public final class GoogleUserEntityTest {

    /**
     * Assert we can get/set the name.
     */
    @Test
    public void getSetName() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        GoogleUserEntity token = new GoogleUserEntity();

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
        GoogleUserEntity token = new GoogleUserEntity();

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
        GoogleUserEntity token = new GoogleUserEntity();

        assertNull(token.getEmail());
        token.setEmail(randomValue);
        assertEquals(randomValue, token.getEmail());
    }

    /**
     * Assert we can get/set the verification state.
     */
    @Test
    public void getSetVerifiedEmail() {
        GoogleUserEntity token = new GoogleUserEntity();

        assertFalse(token.isVerifiedEmail());
        token.setVerifiedEmail(true);
        assertTrue(token.isVerifiedEmail());
    }

    /**
     * Assert we can get/set the familyName.
     */
    @Test
    public void getSetFamilyName() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        GoogleUserEntity token = new GoogleUserEntity();

        assertNull(token.getFamilyName());
        token.setFamilyName(randomValue);
        assertEquals(randomValue, token.getFamilyName());
    }

    /**
     * Assert we can get/set the link.
     */
    @Test
    public void getSetLink() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        GoogleUserEntity token = new GoogleUserEntity();

        assertNull(token.getLink());
        token.setLink(randomValue);
        assertEquals(randomValue, token.getLink());
    }

    /**
     * Assert we can get/set the picture.
     */
    @Test
    public void getSetPicture() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        GoogleUserEntity token = new GoogleUserEntity();

        assertNull(token.getPicture());
        token.setPicture(randomValue);
        assertEquals(randomValue, token.getPicture());
    }

    /**
     * Assert we can get/set the locale.
     */
    @Test
    public void getSetLocale() {
        String randomValue = RandomStringUtils.randomAlphabetic(30);
        GoogleUserEntity token = new GoogleUserEntity();

        assertNull(token.getLocale());
        token.setLocale(randomValue);
        assertEquals(randomValue, token.getLocale());
    }

    /**
     * Assert that we can convert an entity to claims.
     */
    @Test
    public void testAsGenericUser() {
        GoogleUserEntity token = new GoogleUserEntity();

        token.setId(RandomStringUtils.randomAlphabetic(30));
        token.setName(RandomStringUtils.randomAlphabetic(30));
        token.setVerifiedEmail(true);
        token.setFamilyName(RandomStringUtils.randomAlphabetic(30));
        token.setLink(RandomStringUtils.randomAlphabetic(30));
        token.setPicture(RandomStringUtils.randomAlphabetic(30));
        token.setLocale(RandomStringUtils.randomAlphabetic(30));

        OAuth2User user = token.asGenericUser();

        assertEquals(token.getId(), user.getId());
        assertEquals(token.getName(), user.getClaims().get("name"));
        assertEquals(token.isVerifiedEmail().toString(),
                user.getClaims().get("verified_email"));
        assertEquals(token.getFamilyName(),
                user.getClaims().get("family_name"));
        assertEquals(token.getLink(), user.getClaims().get("link"));
        assertEquals(token.getPicture(), user.getClaims().get("picture"));
        assertEquals(token.getLocale(), user.getClaims().get("locale"));
    }
}
