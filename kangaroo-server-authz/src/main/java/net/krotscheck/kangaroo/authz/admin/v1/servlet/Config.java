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

package net.krotscheck.kangaroo.authz.admin.v1.servlet;

/**
 * A utility class filled with all our application configuration keys.
 *
 * @author Michael Krotscheck
 */
public final class Config {

    /**
     * Has the first-run lifecycle listener already executed?
     */
    public static final String FIRST_RUN = "first_run";

    /**
     * Configuration key for the application id.
     */
    public static final String APPLICATION_ID = "application_id";

    /**
     * Configuration key for the initial client id.
     */
    public static final String APPLICATION_CLIENT_ID = "application_client_id";

    /**
     * Configuration key for the system admin id.
     */
    public static final String APPLICATION_ADMIN_ID = "application_admin_id";

    /**
     * Utility class, private constructor.
     */
    private Config() {

    }
}
