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

package net.krotscheck.kangaroo.authz.oauth2.authn;

/**
 * A list of authentication methods which are valid for incoming OAuth2
 * Requests.
 *
 * @author Michael Krotscheck
 */
public enum O2AuthScheme {

    /**
     * This request was authorized via a public client, i.e. a client which
     * does not have an attached secret.
     */
    ClientPublic(false),

    /**
     * This request was authorized via a private client, i.e. a client with
     * a valid paired client secret.
     */
    ClientPrivate(true),

    /**
     * This request was authorized via a bearer token.
     */
    BearerToken(true),

    /**
     * No authentication was included.
     */
    None(false);

    /**
     * Is this an authentication scheme or not.
     */
    private final Boolean auth;

    /**
     * Create an instance.
     *
     * @param auth Is this an authentication scheme, or a public one?
     */
    O2AuthScheme(final Boolean auth) {
        this.auth = auth;
    }

    /**
     * Is this an authentication scheme?
     *
     * @return True if so, otherwise false.
     */
    public Boolean isAuth() {
        return auth;
    }
}
