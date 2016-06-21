/*
 * Copyright (c) 2016 Michael Krotscheck
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
 */

package net.krotscheck.features.database.entity;

/**
 * This represents a registered client, as well as it's connection metadata,
 * for
 * a specific application. Multiple different clients may exist per
 * application.
 *
 * @author Michael Krotscheck
 */
public final class ClientConfig {

    /**
     * Utility method, client configuration.
     */
    private ClientConfig() {

    }

    /**
     * The configuration name for OAuth Token Expiration.
     */
    public static final String AUTHORIZATION_CODE_EXPIRES_NAME
            = "authorization_code_expires_in";

    /**
     * The default value for OAuth Token Expiration (10 minutes).
     */
    public static final Integer AUTHORIZATION_CODE_EXPIRES_DEFAULT =
            60 * 10;

    /**
     * The configuration name for OAuth Token Expiration.
     */
    public static final String ACCESS_TOKEN_EXPIRES_NAME
            = "access_token_expires_in";

    /**
     * The default value for OAuth Token Expiration (10 minutes).
     */
    public static final Integer ACCESS_TOKEN_EXPIRES_DEFAULT =
            60 * 10;

    /**
     * The configuration name for OAuth Token Expiration.
     */
    public static final String REFRESH_TOKEN_EXPIRES_NAME
            = "refresh_token_expires_in";

    /**
     * The default value for OAuth Token Expiration (1 month).
     */
    public static final Integer REFRESH_TOKEN_EXPIRES_DEFAULT =
            60 * 60 * 24 * 30;
}
