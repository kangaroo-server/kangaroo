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

package net.krotscheck.api.oauth.util;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Set;
import javax.ws.rs.core.UriBuilder;

/**
 * A utility filled with validation tools.
 *
 * @author Michael Krotscheck
 */
public final class ValidationUtil {

    /**
     * Utility class, private constructor.
     */
    private ValidationUtil() {

    }

    /**
     * This method assists in determining if a particular URI is valid for
     * the scope of this client.
     *
     * @param redirect  The URI to check.
     * @param redirects A set of redirect url's to check against.
     * @return The validated redirect URI, or null.
     */
    public static URI validateRedirect(final String redirect,
                                       final Set<URI> redirects) {
        // Quick exit
        if (redirects.size() == 0) {
            return null;
        }

        // Can we default?
        if (StringUtils.isEmpty(redirect)) {
            if (redirects.size() == 1) {
                URI[] redirectArray =
                        redirects.toArray(new URI[redirects.size()]);
                return redirectArray[0];
            } else {
                return null;
            }
        }

        // Make sure the passed string is valid.
        URI redirectUri;
        try {
            redirectUri = UriBuilder.fromUri(redirect).build();
        } catch (Exception e) {
            return null;
        }

        // Iterate through the set, comparing as we go.
        for (URI test : redirects) {
            if (test.equals(redirectUri)) {
                return redirectUri;
            }
        }
        return null;
    }
}