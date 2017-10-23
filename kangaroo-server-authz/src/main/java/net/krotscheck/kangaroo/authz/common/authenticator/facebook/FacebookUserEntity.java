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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * A reduced-size POJO of data we're expecting back from facebook. As we do
 * not care about any of the more detailed values of a user (we're only here
 * for Authz right now), we discard everything else.
 *
 * @author Michael Krotscheck
 */
final class FacebookUserEntity {

    /**
     * The id of this person's user account. This ID is unique to each app
     * and cannot be used across different apps. Our upgrade guide provides
     * more information about app-specific IDs
     */
    private String id;

    /**
     * The person's primary email address listed on their profile. This
     * field will not be returned if no valid email address is available
     */
    private String email;

    /**
     * The person's generic name.
     */
    private String name;

    /**
     * The person's first name.
     */
    @JsonProperty("first_name")
    private String firstName;

    /**
     * This person's last name.
     */
    @JsonProperty("last_name")
    private String lastName;

    /**
     * This person's middle name.
     */
    @JsonProperty("middle_name")
    private String middleName;

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
     * Get the first name for this user.
     *
     * @return The user's first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the first name for this user.
     *
     * @param firstName The new first name.
     */
    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    /**
     * Get the last name for this user.
     *
     * @return The user's last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the last name for this user.
     *
     * @param lastName The user's last name.
     */
    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    /**
     * Get the middle name for this user.
     *
     * @return The user's middle name.
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Set the user's middle name.
     *
     * @param middleName The new middle name.
     */
    public void setMiddleName(final String middleName) {
        this.middleName = middleName;
    }

    /**
     * Convert the fields into a claims list.
     *
     * @return A map of claims.
     */
    public Map<String, String> toClaims() {
        HashMap<String, String> outputMap = new HashMap<>();
        outputMap.put("email", email);
        outputMap.put("name", name);
        outputMap.put("firstName", firstName);
        outputMap.put("middleName", middleName);
        outputMap.put("lastName", lastName);

        return outputMap;
    }
}
