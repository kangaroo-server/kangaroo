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

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;

import java.util.HashMap;

/**
 * A reduced-size POJO of data we're expecting back from linkedin.
 *
 * @author Michael Krotscheck
 */
final class LinkedInUserEntity {

    /**
     * The id of this person's user account.
     */
    private String id;

    /**
     * The person's primary email address listed on their profile.
     */
    private String emailAddress;

    /**
     * The person's first name.
     */
    private String firstName;

    /**
     * The person's last name.
     */
    private String lastName;

    /**
     * Get the user's first name.
     *
     * @return The user's first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the user's first name.
     *
     * @param firstName The first name.
     */
    public void setFirstName(final String firstName) {
        this.firstName = firstName;
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
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Set the email for this user.
     *
     * @param emailAddress The user's email.
     */
    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Get the last name.
     *
     * @return The last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the last name.
     *
     * @param lastName New last name.
     */
    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    /**
     * Convert the facebook user to the common user type.
     *
     * @return A map of claims.
     */
    @JsonIgnore
    public OAuth2User asGenericUser() {
        HashMap<String, String> outputMap = new HashMap<>();
        outputMap.put("emailAddress", emailAddress);
        outputMap.put("firstName", firstName);
        outputMap.put("lastName", lastName);

        OAuth2User user = new OAuth2User();
        user.setId(getId());
        user.setClaims(outputMap);
        return user;
    }
}
