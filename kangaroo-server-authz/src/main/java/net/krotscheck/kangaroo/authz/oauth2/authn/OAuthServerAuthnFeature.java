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

import net.krotscheck.kangaroo.authz.oauth2.authn.factory.CredentialsFactory;
import net.krotscheck.kangaroo.authz.oauth2.authn.filter.ClientAuthorizationFilter;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature includes all functionality required to establish a valid
 * authentication context for each request on the OAuth2 server. It includes
 * validating clients, client authentication, browser identification, and
 * maintaining browser session state.
 *
 * @author Michael Krotscheck
 */
public final class OAuthServerAuthnFeature implements Feature {

    /**
     * Register all associated features.
     *
     * @param context The context in which to register features.
     * @return true.
     */
    @Override
    public boolean configure(final FeatureContext context) {
        context.register(new CredentialsFactory.Binder());

        // Service filters
        context.register(new ClientAuthorizationFilter.Binder());

        return true;
    }
}
