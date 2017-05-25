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

package net.krotscheck.kangaroo.authz.admin;

import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * List of all authorization scopes used in this application.
 *
 * @author Michael Krotscheck
 */
public final class Scope {

    /**
     * Utility class, private constructor.
     */
    private Scope() {

    }

    /**
     * Authorization scope for the user resource.
     */
    public static final String USER
            = "kangaroo:user";

    /**
     * Administration scope for the user resource.
     */
    public static final String USER_ADMIN
            = "kangaroo:user_admin";

    /**
     * Authorization scope for the application resource.
     */
    public static final String APPLICATION
            = "kangaroo:application";

    /**
     * Administration scope for the application resource.
     */
    public static final String APPLICATION_ADMIN
            = "kangaroo:application_admin";

    /**
     * Authorization scope for the authenticator resource.
     */
    public static final String AUTHENTICATOR
            = "kangaroo:authenticator";

    /**
     * Authorization scope for the authenticator resource.
     */
    public static final String AUTHENTICATOR_ADMIN =
            "kangaroo:authenticator_admin";

    /**
     * Authorization scope for the client resource.
     */
    public static final String CLIENT
            = "kangaroo:client";

    /**
     * Administration scope for the client resource.
     */
    public static final String CLIENT_ADMIN
            = "kangaroo:client_admin";

    /**
     * Authorization scope for the identity resource.
     */
    public static final String IDENTITY
            = "kangaroo:identity";

    /**
     * Administration scope for the identity resource.
     */
    public static final String IDENTITY_ADMIN
            = "kangaroo:identity_admin";

    /**
     * Authorization scope for the token resource.
     */
    public static final String TOKEN
            = "kangaroo:token";

    /**
     * Administration scope for the token resource.
     */
    public static final String TOKEN_ADMIN
            = "kangaroo:token_admin";

    /**
     * Authorization scope for the roles resource.
     */
    public static final String ROLE = "kangaroo:role";

    /**
     * Administration scope for the roles resource.
     */
    public static final String ROLE_ADMIN = "kangaroo:role_admin";

    /**
     * Authorization scope for the scopes resource (not to be mistaken with
     * this class).
     */
    public static final String SCOPE = "kangaroo:scope";

    /**
     * Administration scope for the scopes resource (not to be mistaken with
     * this class).
     */
    public static final String SCOPE_ADMIN = "kangaroo:scope_admin";

    /**
     * Get a list of all the scopes.
     *
     * @return A list of all scopes.
     */
    public static List<String> allScopes() {
        return Collections.unmodifiableList(
                Arrays.asList(APPLICATION, APPLICATION_ADMIN, AUTHENTICATOR,
                        AUTHENTICATOR_ADMIN, CLIENT, CLIENT_ADMIN, IDENTITY,
                        IDENTITY_ADMIN, ROLE, ROLE_ADMIN, USER, USER_ADMIN,
                        SCOPE, SCOPE_ADMIN, TOKEN, TOKEN_ADMIN));
    }

    /**
     * List of all scopes granted to admins.
     *
     * @return List of all administrator scopes.
     */
    public static List<String> adminScopes() {
        return Collections.unmodifiableList(
                Arrays.asList(APPLICATION_ADMIN, AUTHENTICATOR_ADMIN,
                        CLIENT_ADMIN, IDENTITY_ADMIN, ROLE_ADMIN, USER_ADMIN,
                        SCOPE_ADMIN, TOKEN_ADMIN));
    }

    /**
     * List of all scopes granted to users.
     *
     * @return List of all user scopes.
     */
    public static List<String> userScopes() {
        return Collections.unmodifiableList(
                Arrays.asList(APPLICATION, AUTHENTICATOR, CLIENT, IDENTITY,
                        ROLE, USER, SCOPE, TOKEN));
    }

    /**
     * Determine the required scope for the given entity.
     *
     * @param entity The entity to check.
     * @param admin  Whether to get the admin role or the regular one.
     * @return The name of the scope for admin rights.
     */
    public static String forEntity(final AbstractAuthzEntity entity,
                                   final Boolean admin) {
        if (entity instanceof Application) {
            return admin ? APPLICATION_ADMIN : APPLICATION;
        }
        if (entity instanceof Authenticator) {
            return admin ? AUTHENTICATOR_ADMIN : AUTHENTICATOR;
        }
        if (entity instanceof Client) {
            return admin ? CLIENT_ADMIN : CLIENT;
        }
        if (entity instanceof UserIdentity) {
            return admin ? IDENTITY_ADMIN : IDENTITY;
        }
        if (entity instanceof Role) {
            return admin ? ROLE_ADMIN : ROLE;
        }
        if (entity instanceof User) {
            return admin ? USER_ADMIN : USER;
        }
        if (entity instanceof ApplicationScope) {
            return admin ? SCOPE_ADMIN : SCOPE;
        }
        if (entity instanceof OAuthToken) {
            return admin ? TOKEN_ADMIN : TOKEN;
        }
        return "kangaroo:unknown";
    }
}
