/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.common.authenticator.github;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.ThirdPartyErrorException;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.AbstractOAuth2Authenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2IdPToken;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import java.util.Map;

/**
 * This authentication helper permits using github as an IdP. It's not a
 * true github app: it immediately discards any issued token, and
 * does no subsequent lookups on the user. In other words, this will not
 * provide you with fancy amazing github features, though we may choose to
 * enable this in the future.
 *
 * @author Michael Krotscheck
 */
public final class GithubAuthenticator
        extends AbstractOAuth2Authenticator {

    /**
     * Github's base user endpoint.
     */
    private static final String USER_ENDPOINT =
            "https://api.github.com/user";

    /**
     * Github's auth endpoint.
     *
     * @return The absolute URL to Github's auth endpoint.
     */
    @Override
    protected String getAuthEndpoint() {
        return "https://github.com/login/oauth/authorize";
    }

    /**
     * The token endpoint.
     *
     * @return Github's token endpoint.
     */
    @Override
    protected String getTokenEndpoint() {
        return "https://github.com/login/oauth/access_token";
    }

    /**
     * List of scopes that we need from Github.
     *
     * @return A static list of scopes.
     */
    @Override
    protected String getScopes() {
        return "read:user,user:email";
    }

    /**
     * Load the user's identity from Github, and wrap it into a common format.
     *
     * @param token The OAuth token.
     * @return The user identity.
     */
    @Override
    protected OAuth2User loadUserIdentity(final OAuth2IdPToken token) {
        Response r = getClient()
                .target(USER_ENDPOINT)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getAccessToken()))
                .get();

        try {
            // If this is an error...
            if (r.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                GithubUserEntity githubUser =
                        r.readEntity(GithubUserEntity.class);
                if (githubUser.getId() == null) {
                    throw new ThirdPartyErrorException();
                }
                return githubUser.asGenericUser();
            } else {
                Map<String, String> params = r.readEntity(MAP_TYPE);
                throw new ThirdPartyErrorException(params);
            }
        } catch (ProcessingException e) {
            throw new ThirdPartyErrorException();
        } finally {
            r.close();
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(GithubAuthenticator.class)
                    .to(IAuthenticator.class)
                    .named(AuthenticatorType.Github.name())
                    .in(RequestScoped.class);
        }
    }
}
