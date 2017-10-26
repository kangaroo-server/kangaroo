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

import java.util.HashMap;
import java.util.Map;

/**
 * A shared user entity that asks each IdP to package its identity claims in
 * a common format. With this we can then map our remote identities to the
 * internal ones.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2User {

    /**
     * The id of this person's remote user account.
     */
    private String id;

    /**
     * A list of claims.
     */
    private Map<String, String> claims = new HashMap<>();

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
     * Return the claims for this identity.
     *
     * @return This identity's claims.
     */
    public Map<String, String> getClaims() {
        return claims;
    }

    /**
     * Set new claims for this identity.
     *
     * @param claims The new claims.
     */
    public void setClaims(final Map<String, String> claims) {
        this.claims = new HashMap<>(claims);
    }
}
