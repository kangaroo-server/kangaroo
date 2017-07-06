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

import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Set;

/**
 * Private security context implementation that validates against our
 * database of tokens.
 *
 * @author Michael Krotscheck
 */
// TODO(krotscheck): This class shouldn't depend on an active DB session.
public final class OAuth2SecurityContext implements SecurityContext {

    /**
     * Is this secure?
     */
    private final boolean secure;

    /**
     * The principal.
     */
    private final OAuthToken principal;

    /**
     * The scopes.
     */
    private final Set<String> scopes;

    /**
     * Construct an authentication context from an OAuth token and a secure
     * flag.
     *
     * @param token    The OAuth token for this principal.
     * @param isSecure Whether to secure the context.
     */
    public OAuth2SecurityContext(final OAuthToken token,
                                 final Boolean isSecure) {
        // Materialize the scopes and the user identity.
        principal = token;
        scopes = token.getScopes().keySet();
        secure = isSecure;
    }

    /**
     * Return the current user identity.
     */
    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    /**
     * WARNING: OVERLOADED TERMS
     * <p>
     * In order to simplify the declaration of scope permissions, this
     * method will check to see if the current user has been granted the
     * provied "scope" rather than "role".
     */
    @Override
    public boolean isUserInRole(final String roleName) {
        return scopes.contains(roleName);
    }

    /**
     * Was this request done via a secure request method?
     */
    @Override
    public boolean isSecure() {
        return secure;
    }

    /**
     * Get the authentication scheme.
     */
    @Override
    public String getAuthenticationScheme() {
        return "OAuth2";
    }
}
