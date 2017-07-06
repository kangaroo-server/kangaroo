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

package net.krotscheck.kangaroo.authz.admin.v1.auth;

import net.krotscheck.kangaroo.authz.admin.v1.auth.exception.WWWChallengeExceptionMapper;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature ensures that all entities that are annotated with the
 * `@ScopesAllowed` tag, have appropriate HTTP exceptions and WWW-Authenticate
 * challenges thrown in response to invalid requests.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2AuthFeature implements Feature {

    /**
     * Configure this feature.
     *
     * @param context The context to configure.
     * @return true.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // The dynamic feature which provides per-resource authz/authn filters.
        context.register(OAuth2ScopeDynamicFeature.class);

        // The error response writer that knows how to return the above
        // exceptions to the client.
        context.register(new WWWChallengeExceptionMapper.Binder());

        return true;
    }
}
