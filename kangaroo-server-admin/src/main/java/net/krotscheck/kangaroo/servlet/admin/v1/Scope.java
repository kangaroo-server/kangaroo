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

package net.krotscheck.kangaroo.servlet.admin.v1;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * List of all authorization scopes used in this application.
 *
 * @author Michael Krotscheck
 */
public final class Scope {

    /**
     * Utility class, private constructor.
     */
    private Scope() {

    }

    /**
     * Authorization scope for the user resource.
     */
    public static final String USER = "kangaroo:user";

    /**
     * Authorization scope for the application resource.
     */
    public static final String APPLICATION = "kangaroo:application";

    /**
     * Authorization scope for the authenticator resource.
     */
    public static final String AUTHENTICATOR = "kangaroo:authenticator";

    /**
     * Authorization scope for the client resource.
     */
    public static final String CLIENT = "kangaroo:client";

    /**
     * Authorization scope for the identity resource.
     */
    public static final String IDENTITY = "kangaroo:identity";

    /**
     * Authorization scope for the roles resource.
     */
    public static final String ROLE = "kangaroo:role";

    /**
     * Get a list of all the scopes.
     *
     * @return A list of all scopes.
     */
    public static List<String> allScopes() {
        return Collections.unmodifiableList(
                Arrays.asList(APPLICATION, AUTHENTICATOR, CLIENT, IDENTITY,
                        ROLE, USER));
    }
}
