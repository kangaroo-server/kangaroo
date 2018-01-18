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

import com.google.common.base.Objects;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.common.hibernate.entity.AbstractEntity;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static net.krotscheck.kangaroo.util.ObjectUtil.safeCast;

/**
 * The authorization pricipal for the OAuth server.
 *
 * @author Michael Krotscheck
 */
public final class O2Principal implements Principal {

    /**
     * The client within whose scope we are acting.
     */
    private final Client contextClient;

    /**
     * The token used to construct this principal.
     */
    private final OAuthToken oAuthToken;

    /**
     * Create a new Principal with no authentication.
     */
    public O2Principal() {
        this(null, null);
    }

    /**
     * Create a new principal from a authClient.
     *
     * @param client A authClient.
     */
    public O2Principal(final Client client) {
        this(client, null);
    }

    /**
     * Create a new principal from an OAuth token. Note that this constructor
     * does not validate the input parameter.
     *
     * @param token The token to wrap.
     */
    public O2Principal(final OAuthToken token) {
        this(Optional.ofNullable(token)
                        .map(OAuthToken::getClient)
                        .orElse(null),
                token);
    }

    /**
     * Consolidation constructor.
     *
     * @param client The client.
     * @param token  The token.
     */
    private O2Principal(final Client client, final OAuthToken token) {
        this.contextClient = Optional.ofNullable(client)
                .filter(c -> !isNull(c.getId()))
                .orElse(null);
        this.oAuthToken = Optional.ofNullable(token)
                .filter(t -> !isNull(t.getId()))
                .orElse(null);
    }

    /**
     * Retrieve the OAuth token.
     *
     * @return The wrapped token.
     */
    public OAuthToken getOAuthToken() {
        return oAuthToken;
    }

    /**
     * Assert that only one entity is set, none, or that both are identical.
     *
     * @param left  A left entity.
     * @param right A right entity.
     * @param <T>   Raw type of abstract entity, used to determine return type.
     * @return The set entity, or null.
     * @throws AccessDeniedException If both are set.
     */
    protected <T extends AbstractEntity> T sameOrOne(final T left,
                                                     final T right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (!left.equals(right)) {
            throw new AccessDeniedException();
        }
        return left;
    }

    /**
     * Return the name of the current principal.
     *
     * @return The 'name' of the current principal.
     */
    @Override
    public String getName() {
        if (!isNull(contextClient)) {
            return contextClient.getName();
        }
        return null;
    }

    /**
     * Implication assertion. Not implemented.
     *
     * @param subject The subject.
     * @return Always returns false.
     */
    @Override
    public boolean implies(final Subject subject) {
        return false;
    }

    /**
     * Return the authentication scheme used for this principal.
     *
     * @return The principal.
     */
    public String getScheme() {
        if (oAuthToken == null) {
            return Optional.ofNullable(contextClient)
                    .map(c -> c.isPublic() ? O2AuthScheme.ClientPublic
                            : O2AuthScheme.ClientPrivate)
                    .orElse(O2AuthScheme.None)
                    .toString();
        } else {
            return O2AuthScheme.BearerToken.toString();
        }
    }

    /**
     * The client request context.
     *
     * @return The client within whose context we should evaluate this request.
     */
    public Client getContext() {
        return contextClient;
    }

    /**
     * Merge a new principal's values into this principal. This method will
     * throw exceptions based on conflicts, or will return a new principal
     * instance that contains the merged values.
     *
     * @param newPrincipal The new principal values to merge into the
     *                     existing one.
     * @return A new principal.
     */
    public O2Principal merge(final Principal newPrincipal) {
        O2Principal principal = safeCast(newPrincipal, O2Principal.class)
                .orElse(null);

        // If the principal is empty, or not the correct type, exit.
        if (isNull(principal)) {
            return new O2Principal(contextClient, oAuthToken);
        }

        // First, create a new one.
        O2Principal merged = new O2Principal(
                // force merge the clients.
                sameOrOne(contextClient, principal.contextClient),
                // force merge the clients.
                sameOrOne(oAuthToken, principal.oAuthToken)
        );

        // See if there's a scheme mismatch between current and incoming.
        long validSchemes = Stream.of(getScheme(), principal.getScheme())
                .map(O2AuthScheme::valueOf)
                .distinct()
                .filter(s -> !s.equals(O2AuthScheme.None))
                .count();

        // Scheme escalation checks, sort the schemes, filter out
        // low-priority ones, and error if more than one is found.
        if (validSchemes > 1) {
            throw new AccessDeniedException();
        }
        return merged;
    }

    /**
     * Assert equality between objects based on values.
     *
     * @param o The value to check.
     * @return True if they are equal, otherwise false.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return safeCast(o, O2Principal.class)
                .filter(p -> Objects.equal(getScheme(),
                        p.getScheme()))
                .filter(p -> Objects.equal(getContext(),
                        p.getContext()))
                .filter(p -> Objects.equal(getOAuthToken(),
                        p.getOAuthToken()))
                .isPresent();
    }

    /**
     * Generate a hash code for this entity.
     *
     * @return The generated hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getScheme(), getContext(), getOAuthToken());
    }
}
