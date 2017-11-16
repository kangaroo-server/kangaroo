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

package net.krotscheck.kangaroo.test.jersey;

import org.mockito.internal.util.collections.Sets;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.util.Set;

/**
 * A client API filter which automatically applies the X-Requested-With header.
 *
 * @author Michael Krotscheck
 */
public final class CsrfProtectionFilter implements ClientRequestFilter {

    /**
     * Name of the header this filter will attach to the request.
     */
    public static final String HEADER = "X-Requested-With";

    /**
     * Request methods to ignore.
     */
    private static final Set<String> METHODS_TO_IGNORE
            = Sets.newSet("GET", "OPTIONS", "HEAD");

    /**
     * Apply the header.
     *
     * @param request The request.
     */
    @Override
    public void filter(final ClientRequestContext request) {
        if (!METHODS_TO_IGNORE.contains(request.getMethod())
                && !request.getHeaders().containsKey(HEADER)) {
            request.getHeaders().add(HEADER, "Kangaroo Test Harness");
        }
    }
}

