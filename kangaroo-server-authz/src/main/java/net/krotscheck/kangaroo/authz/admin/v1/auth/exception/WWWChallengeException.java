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

package net.krotscheck.kangaroo.authz.admin.v1.auth.exception;

import net.krotscheck.kangaroo.common.exception.KangarooException;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * Private, common implementation of all exceptions that need to return a
 * WWW-Authenticate challenge.
 *
 * @author Michael Krotscheck
 */
public abstract class WWWChallengeException extends KangarooException {

    /**
     * List of required scopes for this resource.
     */
    private final String[] requiredScopes;

    /**
     * The URI Realm.
     */
    private final URI realm;

    /**
     * Create a new exception with the specified error code.
     *
     * @param code           The error code enum type.
     * @param requestInfo    The original URI request, from which we're
     *                       going to derive our realm.
     * @param requiredScopes A list of required scopes.
     */
    protected WWWChallengeException(final ErrorCode code,
                                    final UriInfo requestInfo,
                                    final String[] requiredScopes) {
        super(code);
        this.requiredScopes = requiredScopes;

        UriBuilder realmBuilder = requestInfo.getBaseUriBuilder();
        List<String> paths = requestInfo.getMatchedURIs();
        if (paths.size() > 0) {
            realmBuilder.path(paths.get(paths.size() - 1));
        }
        this.realm = realmBuilder.build();
    }

    /**
     * Get the required scopes.
     *
     * @return List of required scopes.
     */
    public final String[] getRequiredScopes() {
        return requiredScopes;
    }

    /**
     * The URI Realm.
     *
     * @return URI of the authorization realm.
     */
    public final URI getRealm() {
        return realm;
    }
}
