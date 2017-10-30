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

import net.krotscheck.kangaroo.authz.common.authenticator.facebook.FacebookAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.google.GoogleAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.linkedin.LinkedInAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.password.PasswordAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.test.TestAuthenticator;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This module includes all authenticators build for the OAuth system.
 *
 * @author Michael Krotscheck
 */
public final class AuthenticatorFeature implements Feature {

    /**
     * Register this feature.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Authenticators.
        context.register(new TestAuthenticator.Binder());
        context.register(new PasswordAuthenticator.Binder());
        context.register(new GoogleAuthenticator.Binder());
        context.register(new FacebookAuthenticator.Binder());
        context.register(new LinkedInAuthenticator.Binder());

        return true;
    }
}
