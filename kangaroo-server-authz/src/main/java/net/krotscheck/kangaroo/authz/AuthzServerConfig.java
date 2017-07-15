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

package net.krotscheck.kangaroo.authz;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

/**
 * List of configuration settings specific to the Authz server.
 *
 * @author Michael Krotscheck
 */
public final class AuthzServerConfig {

    /**
     * The name of the cookie used to maintain implicit flow sessions.
     */
    public static final Entry<String, String> SESSION_NAME = new
            SimpleImmutableEntry<>("kangaroo.authz_session_name", "kangaroo");

    /**
     * The maximum time that the session should live.
     */
    public static final Entry<String, Integer> SESSION_MAX_AGE = new
            SimpleImmutableEntry<>("kangaroo.authz_session_max_age",
            60 * 60 * 24); // Cookie lasts for 24 hours

    /**
     * Private constructor for a utility class.
     */
    private AuthzServerConfig() {

    }
}
