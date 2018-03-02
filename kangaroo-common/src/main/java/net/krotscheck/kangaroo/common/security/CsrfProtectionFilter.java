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

import com.google.common.collect.Sets;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.util.Set;

/**
 * A CSRFProtection filter which, unlike Jersey2's, uses the X-Requested-With
 * header as a check. To check whether this code still needs to exist, please
 * see: https://github.com/jersey/jersey/issues/3717
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHENTICATION)
public final class CsrfProtectionFilter implements ContainerRequestFilter {

    /**
     * The header we use as a sanity check.
     */
    private static final String HEADER = "X-Requested-With";

    /**
     * Short list of methods to ignore.
     */
    private static final Set<String> METHODS_TO_IGNORE =
            Sets.newHashSet("GET", "OPTIONS", "HEAD");

    /**
     * Check the request for the header.
     *
     * @param request The request context.
     */
    @Override
    public void filter(final ContainerRequestContext request) {
        if (METHODS_TO_IGNORE.contains(request.getMethod())) {
            return;
        }

        if (request.getHeaders().containsKey(HEADER)) {
            return;
        }

        throw new NoCSRFHeaderException();
    }
}
