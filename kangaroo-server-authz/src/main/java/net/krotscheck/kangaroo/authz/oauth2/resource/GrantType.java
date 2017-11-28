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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;

import java.util.Arrays;
import java.util.List;

/**
 * All the grant types supported by the token endpoint.
 *
 * @author Michael Krotscheck
 */
public enum GrantType {

    /**
     * Token grant type: 'authorization_code'.
     */
    AuthorizationCode("authorization_code"),

    /**
     * Token grant type: 'client_credentials'.
     */
    ClientCredentials("client_credentials"),

    /**
     * Token grant type: 'password'.
     */
    Password("password"),

    /**
     * Token grant type: 'refresh_token'.
     */
    RefreshToken("refresh_token");

    /**
     * All the types.
     */
    private static final List<GrantType> ALL = Arrays.asList(AuthorizationCode,
            ClientCredentials, Password, RefreshToken);
    /**
     * The value, also used as the URL parameter.
     */
    private final String value;

    /**
     * Create a new grant type with a specific string.
     *
     * @param value The value.
     */
    GrantType(final String value) {
        this.value = value;
    }

    /**
     * Convert a string into an Enum instance, used for jersey deserialization.
     *
     * @param value The value to interpret.
     * @return The enum instance.
     */
    public static GrantType fromString(final String value) {
        for (GrantType type : ALL) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new InvalidGrantException();
    }

    /**
     * Return the string representation of this enum.
     *
     * @return The string representation.
     */
    public String toString() {
        return value;
    }
}
