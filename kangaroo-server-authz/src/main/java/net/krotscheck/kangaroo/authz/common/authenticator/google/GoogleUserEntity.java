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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;

import java.util.HashMap;

/**
 * A reduced-size POJO of data we're expecting back from google.
 *
 * @author Michael Krotscheck
 */
final class GoogleUserEntity {

    /**
     * The id of this person's user account.
     */
    private String id;

    /**
     * The person's primary email address listed on their profile.
     */
    private String email;

    /**
     * Is this email address verified?
     */
    @JsonProperty("verified_email")
    private Boolean verifiedEmail = false;

    /**
     * The person's generic name.
     */
    private String name;

    /**
     * The person's family name.
     */
    @JsonProperty("family_name")
    private String familyName;

    /**
     * The person's google plus link.
     */
    private String link;

    /**
     * The person's profile image.
     */
    private String picture;

    /**
     * The person's locale.
     */
    private String locale;

    /**
     * Get the user's name.
     *
     * @return The user's generic name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the user's name.
     *
     * @param name The user name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the application specific user id of this user.
     *
     * @return The user id.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the application-specific user id for this user.
     *
     * @param id The user id.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Get the email for this user.
     *
     * @return The user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the email for this user.
     *
     * @param email The user's email.
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * Is this email verified?
     *
     * @return True if verified, otherwise false.
     */
    public Boolean isVerifiedEmail() {
        return verifiedEmail;
    }

    /**
     * Set the user's verified email state.
     *
     * @param verifiedEmail True if it's verified, otherwise false.
     */
    public void setVerifiedEmail(final Boolean verifiedEmail) {
        this.verifiedEmail = verifiedEmail;
    }

    /**
     * Get the family name.
     *
     * @return The family name.
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Set the family name.
     *
     * @param familyName New family name.
     */
    public void setFamilyName(final String familyName) {
        this.familyName = familyName;
    }

    /**
     * Get the google plus link.
     *
     * @return Google plus link.
     */
    public String getLink() {
        return link;
    }

    /**
     * Set the google plus link.
     *
     * @param link Google plus link.
     */
    public void setLink(final String link) {
        this.link = link;
    }

    /**
     * Get the link to the user's profile picture.
     *
     * @return The user's profile picture.
     */
    public String getPicture() {
        return picture;
    }

    /**
     * Set a link to the user's profile picture.
     *
     * @param picture The profile picture.
     */
    public void setPicture(final String picture) {
        this.picture = picture;
    }

    /**
     * Get the user's locale.
     *
     * @return The user's locale.
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Set the locale.
     *
     * @param locale The locale.
     */
    public void setLocale(final String locale) {
        this.locale = locale;
    }

    /**
     * Convert the facebook user to the common user type.
     *
     * @return A map of claims.
     */
    @JsonIgnore
    public OAuth2User asGenericUser() {
        HashMap<String, String> outputMap = new HashMap<>();
        outputMap.put("email", email);
        outputMap.put("name", name);
        outputMap.put("verified_email", String.valueOf(verifiedEmail));
        outputMap.put("family_name", familyName);
        outputMap.put("link", link);
        outputMap.put("picture", picture);
        outputMap.put("locale", locale);

        OAuth2User user = new OAuth2User();
        user.setId(getId());
        user.setClaims(outputMap);
        return user;
    }
}
