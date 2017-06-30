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

package net.krotscheck.kangaroo.common.security;

import org.glassfish.jersey.server.filter.CsrfProtectionFilter;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This Jersey2 Feature contains a mishmash of services, filters, and other
 * components that loosely fall under the umbrella of "security".
 *
 * @author Michael Krotscheck
 */
public final class SecurityFeature implements Feature {

    /**
     * Register all security-related components.
     *
     * @param context configurable context in which the feature should be
     *                enabled.
     * @return {@code true} if the feature was successfully enabled.
     */
    @Override
    public boolean configure(final FeatureContext context) {
        context.register(new SecurityHeadersFilter.Binder());
        context.register(CsrfProtectionFilter.class);
        return true;
    }
}
