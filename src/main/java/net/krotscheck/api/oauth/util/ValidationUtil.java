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

import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.features.database.entity.ApplicationScope;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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

    /**
     * Creates a collection of scopes from a list of valid scopes. If the
     * requested scopes are not in that valid list, it will throw an exception.
     *
     * @param requestedScopes An array of requested scopes.
     * @param validScopes     A list of valid scopes.
     * @return A list of the requested scopes, as database instances.
     */
    public static SortedMap<String, ApplicationScope> validateScope(
            final String[] requestedScopes,
            final SortedMap<String, ApplicationScope> validScopes) {

        if (requestedScopes == null || requestedScopes.length == 0) {
            return new TreeMap<>();
        }

        if (validScopes == null) {
            throw new InvalidScopeException();
        }

        // Make sure all requested scopes are in the map.
        SortedMap<String, ApplicationScope> results = new TreeMap<>();
        for (String scope : requestedScopes) {
            if (!validScopes.containsKey(scope)) {
                throw new InvalidScopeException();
            }
            results.put(scope, validScopes.get(scope));
        }
        return results;
    }

    /**
     * Creates a collection of scopes from a list of valid scopes. If the
     * requested scopes are not in that valid list, it will throw an exception.
     *
     * @param requestedScopes An array of requested scopes.
     * @param validScopes     A string of valid scopes.
     * @return A list of the requested scopes, as database instances.
     */
    public static SortedMap<String, ApplicationScope> validateScope(
            final String requestedScopes,
            final SortedMap<String, ApplicationScope> validScopes) {
        if (StringUtils.isEmpty(requestedScopes)) {
            return new TreeMap<>();
        }
        return validateScope(requestedScopes.split(" "), validScopes);
    }

    /**
     * Revalidates a list of provided scopes against the originally granted
     * scopes, as well as the current list of valid scopes. If the list of
     * valid scopes has changed since the original grant list, any missing
     * scopes will be quietly dropped.
     *
     * @param requestedScopes An array of requested scopes.
     * @param originalScopes  The original set of scopes.
     * @param validScopes     The current list of valid scopes.
     * @return A list of the requested scopes, as database instances.
     */
    public static SortedMap<String, ApplicationScope> revalidateScope(
            final String[] requestedScopes,
            final SortedMap<String, ApplicationScope> originalScopes,
            final SortedMap<String, ApplicationScope> validScopes) {

        if (validScopes == null || originalScopes == null) {
            throw new InvalidScopeException();
        }

        if (requestedScopes == null || requestedScopes.length == 0) {
            return new TreeMap<>();
        }

        // Reduce the valid scope list down by the original scopes.
        SortedMap<String, ApplicationScope> results = new TreeMap<>();
        for (String scope : requestedScopes) {
            if (validScopes.containsKey(scope)) {
                if (!originalScopes.containsKey(scope)) {
                    throw new InvalidScopeException();
                }
                results.put(scope, validScopes.get(scope));
            }
        }

        return results;
    }

    /**
     * Revalidates a list of provided scopes against the originally granted
     * scopes, as well as the current list of valid scopes. If the list of
     * valid scopes has changed since the original grant list, any missing
     * scopes will be quietly dropped.
     *
     * @param requestedScopes An array of requested scopes.
     * @param originalScopes  The original set of scopes.
     * @param validScopes     The current list of valid scopes.
     * @return A list of the requested scopes, as database instances.
     */
    public static SortedMap<String, ApplicationScope> revalidateScope(
            final String requestedScopes,
            final SortedMap<String, ApplicationScope> originalScopes,
            final SortedMap<String, ApplicationScope> validScopes) {
        if (StringUtils.isEmpty(requestedScopes)) {
            return new TreeMap<>();
        }
        return revalidateScope(requestedScopes.split(" "),
                originalScopes,
                validScopes);
    }
}
