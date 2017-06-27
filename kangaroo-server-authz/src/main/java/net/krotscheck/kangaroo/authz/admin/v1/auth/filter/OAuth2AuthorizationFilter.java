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

package net.krotscheck.kangaroo.authz.admin.v1.auth.filter;

import net.krotscheck.kangaroo.authz.admin.v1.auth.exception.OAuth2ForbiddenException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Arrays;

/**
 * This filter checks the user principal on every request, to see whether it
 * posesses the appropriate "scopes". For the sake of expediency, we're
 * conflating "roles" and "scopes" here, as the methods for the former
 * already exist.
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHORIZATION)
public final class OAuth2AuthorizationFilter
        implements ContainerRequestFilter {

    /**
     * Short list of roles allowed for this particular resource.
     */
    private final String[] scopesAllowed;

    /**
     * Create a new filter, denying all requests.
     */
    public OAuth2AuthorizationFilter() {
        this(new String[]{});
    }

    /**
     * Create a new filter, only permitting the provided scopes.
     *
     * @param scopesAllowed The permitted scopes.
     */
    public OAuth2AuthorizationFilter(final String[] scopesAllowed) {
        this.scopesAllowed = scopesAllowed;
    }

    /**
     * Handle the request.
     *
     * @param requestContext Request context.
     * @throws IOException Should (hopefully) not be thrown.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext)
            throws IOException {

        SecurityContext context = requestContext.getSecurityContext();

        if (context == null) {
            throw new OAuth2ForbiddenException(requestContext.getUriInfo(),
                    scopesAllowed);
        }

        long matchedScopes = Arrays.stream(scopesAllowed)
                .filter(context::isUserInRole)
                .count();

        if (matchedScopes == 0) {
            throw new OAuth2ForbiddenException(requestContext.getUriInfo(),
                    scopesAllowed);
        }
    }
}
