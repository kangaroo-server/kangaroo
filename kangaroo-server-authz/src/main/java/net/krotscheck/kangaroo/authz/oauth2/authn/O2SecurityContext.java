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

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * The security context we're using to manage our authorization.
 *
 * @author Michael Krotscheck
 */
public final class O2SecurityContext implements SecurityContext {

    /**
     * The principal for this context.
     */
    private final O2Principal principal;

    /**
     * Create a new instance of the security context.
     *
     * @param principal The principal that should be wrapped.
     */
    public O2SecurityContext(final O2Principal principal) {
        this.principal = principal;
    }

    /**
     * Return the active user principal.
     *
     * @return The active user principal.
     */
    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    /**
     * This doesn't do anything for this implementation.
     *
     * @param role A 'role'.
     * @return Always false.
     */
    @Override
    public boolean isUserInRole(final String role) {
        return false;
    }

    /**
     * Is this request secure (i.e. via HTTPS/TLS) or not?
     *
     * @return True if secure, otherwise false.
     */
    @Override
    public boolean isSecure() {
        return true;
    }

    /**
     * Return the method by which this context was authenticated.
     *
     * @return A string representing the security method.
     */
    @Override
    public String getAuthenticationScheme() {
        return principal.getScheme();
    }
}
