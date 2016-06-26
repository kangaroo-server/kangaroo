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

package net.krotscheck.kangaroo.servlet.oauth2;


import net.krotscheck.test.TestAuthenticator;

/**
 * This application
 *
 * @author Michael Krotscheck
 */
public class OAuthTestApp extends OAuthAPI {

    /**
     * Constructor. Creates a new application instance.
     */
    public OAuthTestApp() {
        super(); // Initialize all the parent things.

        // Add the test authenticator
        register(new TestAuthenticator.Binder());
    }
}
