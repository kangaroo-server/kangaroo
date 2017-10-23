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

import net.krotscheck.kangaroo.authz.common.authenticator.exception.MisconfiguredAuthenticatorException;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;

import java.util.Map;

/**
 * A simple POJO and factory that assists in extracting facebook's required
 * configuration from the Authenticator Configuration.
 *
 * @author Michael Krotscheck
 */
public final class FacebookConfiguration {

    /**
     * The config storage key for the facebook client id.
     */
    protected static final String CLIENT_ID_KEY = "facebook.client_id";

    /**
     * The config storage key for the facebook client secret.
     */
    protected static final String CLIENT_SECRET_KEY = "facebook.client_secret";

    /**
     * The client ID.
     */
    private String clientId;

    /**
     * The facebook api secret.
     */
    private String clientSecret;

    /**
     * Factory generated class; private constructor.
     */
    private FacebookConfiguration() {
    }

    /**
     * POJO factory method, extracts the parameters from the passed
     * configuration and validates them.
     *
     * @param authenticator The configuration instance to parse.
     * @return A configuration instance, or an exception.
     */
    public static FacebookConfiguration from(final Authenticator
                                                     authenticator) {
        if (authenticator == null || authenticator.getConfiguration() == null) {
            throw new MisconfiguredAuthenticatorException();
        }
        Map<String, String> config = authenticator.getConfiguration();
        FacebookConfiguration c = new FacebookConfiguration();

        c.clientId = config.getOrDefault(CLIENT_ID_KEY, null);
        c.clientSecret = config.getOrDefault(CLIENT_SECRET_KEY, null);

        if (c.clientSecret == null || c.clientId == null) {
            throw new MisconfiguredAuthenticatorException();
        }

        return c;
    }

    /**
     * Get the client ID.
     *
     * @return The client id.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Get the client secret.
     *
     * @return The client secret.
     */
    public String getClientSecret() {
        return clientSecret;
    }
}
