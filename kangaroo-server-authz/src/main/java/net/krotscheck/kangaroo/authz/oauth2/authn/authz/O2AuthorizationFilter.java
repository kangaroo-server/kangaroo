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

package net.krotscheck.kangaroo.authz.oauth2.authn.authz;

import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.util.ObjectUtil;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

/**
 * This filter evaluates the existing security context to see whether the
 * request is permitted. It accepts two parameters, permitPrivate and
 * permitPublic, which will govern whether a particular request is allowed to
 * pass.
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHORIZATION)
public final class O2AuthorizationFilter
        implements ContainerRequestFilter {

    /**
     * Whether to permit private clients.
     */
    private final boolean permitPrivate;

    /**
     * Whether to permit public clients.
     */
    private final boolean permitPublic;

    /**
     * Create a new authorization filter, which rejects requests based on the
     * passed authorization flags.
     *
     * @param permitPrivate Whether to permit private clients.
     * @param permitPublic  Whether to permit public clients.
     */
    public O2AuthorizationFilter(final boolean permitPrivate,
                                 final boolean permitPublic) {
        this.permitPrivate = permitPrivate;
        this.permitPublic = permitPublic;
    }

    /**
     * Extract the client ID from the various locations where it can live,
     * and attempt to resolve the client.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext) {
        SecurityContext context = requestContext.getSecurityContext();
        O2Principal principal = ObjectUtil
                .safeCast(context.getUserPrincipal(), O2Principal.class)
                .orElse(null);

        if (principal == null) {
            throw new AccessDeniedException();
        }

        if (principal.getContext() == null) {
            throw new AccessDeniedException();
        }

        boolean isPrivate = principal.getContext().isPrivate();

        // Reject private clients if they're not permitted.
        if (isPrivate && !permitPrivate) {
            throw new AccessDeniedException();
        }

        // Reject public clients if they're not permitted.
        if (!isPrivate && !permitPublic) {
            throw new AccessDeniedException();
        }
    }
}
