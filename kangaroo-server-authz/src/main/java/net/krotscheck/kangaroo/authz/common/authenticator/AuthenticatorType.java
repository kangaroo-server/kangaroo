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

package net.krotscheck.kangaroo.authz.common.authenticator;

/**
 * An enumeration of authenticator types, to be used for injection and access
 * in the database.
 *
 * @author Michael Krotscheck
 */
public enum AuthenticatorType {

    /**
     * This authenticator uses Facebook's OAuth2 protocol as a simple IdP,
     * discarding the received tokens after use.
     */
    Facebook(true),

    /**
     * This authenticator uses Google's OAuth2 protocol as a simple IdP,
     * discarding the received tokens after use.
     */
    Google(true),

    /**
     * This type describes the password authenticator, which drives our Owner
     * Credentials flow. It is considered a private authenticator type, and
     * should not be permitted to be manually set via the admin API.
     */
    Password(true),

    /**
     * The test authenticator type, used mostly for our test harness. Also a
     * private type, and should not be permitted to be attached to a client.
     */
    Test(true);

    /**
     * Is this a private or a public authenticator type?
     */
    private Boolean isAuthenticatorPrivate;

    /**
     * Create a new enum. Private constructor.
     *
     * @param isAuthenticatorPrivate Whether this is a private, or public,
     *                               authenticator. The latter may be
     *                               manipulated via the admin API.
     */
    AuthenticatorType(final Boolean isAuthenticatorPrivate) {
        this.isAuthenticatorPrivate = isAuthenticatorPrivate;
    }

    /**
     * Is this a private authenticator or not?
     *
     * @return Boolean if it is private, otherwise false.
     */
    public Boolean isPrivate() {
        return isAuthenticatorPrivate;
    }

    /**
     * Return true if the current type is in a list of types.
     *
     * @param types The list of types to check.
     * @return True if the type is found, otherwise false.
     */
    public Boolean in(final AuthenticatorType... types) {
        if (types == null) {
            return false;
        }
        for (AuthenticatorType type : types) {
            if (this.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
