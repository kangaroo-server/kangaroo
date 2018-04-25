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

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;

import java.util.HashMap;

/**
 * A reduced-size POJO of data we're expecting back from github.
 *
 * @author Michael Krotscheck
 */
final class GithubUserEntity {

    /**
     * The id of this person's user account.
     */
    private Integer id;

    /**
     * The user's login.
     */
    private String login;

    /**
     * The user's email.
     */
    private String email;

    /**
     * The user's name.
     */
    private String name;

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
     * Get the user id of this user.
     *
     * @return The user id.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Set the id for this user.
     *
     * @param id The user id.
     */
    public void setId(final Integer id) {
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
     * Get the user's login.
     *
     * @return The user's github login.
     */
    public String getLogin() {
        return login;
    }

    /**
     * Set the user's github login.
     *
     * @param login The new login.
     */
    public void setLogin(final String login) {
        this.login = login;
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
        outputMap.put("login", login);

        OAuth2User user = new OAuth2User();
        user.setId(getId().toString());
        user.setClaims(outputMap);
        return user;
    }
}
