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

package net.krotscheck.kangaroo.authz.common.authenticator.linkedin;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Uninterruptibles;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.ThirdPartyErrorException;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.AbstractOAuth2Authenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2IdPToken;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This authenticator permits using LinkedIn as an IdP. It does not pass the
 * linkedin token through; only uses it to query basic profile information
 * and verify identity.
 *
 * @author Michael Krotscheck
 */
public final class LinkedInAuthenticator
        extends AbstractOAuth2Authenticator {

    /**
     * LinkedIn's auth endpoint.
     *
     * @return The absolute URL to linkedin's auth endpoint.
     */
    @Override
    protected String getAuthEndpoint() {
        return "https://www.linkedin.com/oauth/v2/authorization";
    }

    /**
     * LinkedIn's token endpoint.
     *
     * @return The absolute URL to linkedin's token endpoint.
     */
    @Override
    protected String getTokenEndpoint() {
        return "https://www.linkedin.com/oauth/v2/accessToken";
    }

    /**
     * List of scopes that we need from linkedin.
     *
     * @return A static list of scopes.
     */
    @Override
    protected String getScopes() {
        return StringUtils.join(new String[]{
                "r_basicprofile",
                "r_emailaddress"
        }, " ");
    }

    /**
     * Retrieve the user info from LinkedIn.
     *
     * @param token The OAuth token.
     * @return The remote user data from LinkedIn.
     */
    @Override
    protected OAuth2User loadUserIdentity(final OAuth2IdPToken token) {
        int count = 0;
        int maxTries = 5;

        while (true) {
            try {
                return tryUserLoad(token);
            } catch (ThirdPartyErrorException e) {
                if (++count == maxTries) {
                    throw e;
                }
                Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Try to load the user from linkedin. If it fails, throw an exception.
     *
     * @param token The oauth2 token to use.
     * @return The user, or an exception.
     */
    private OAuth2User tryUserLoad(final OAuth2IdPToken token) {
        Response r = getClient()
                .target("https://api.linkedin.com/v1/people/~"
                        + ":(id,first-name,last-name,email-address)")
                .request()
                .header("x-li-format", "json")
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getAccessToken()))
                .get();

        try {
            // If this is an error...
            if (r.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                LinkedInUserEntity gUser =
                        r.readEntity(LinkedInUserEntity.class);
                if (Strings.isNullOrEmpty(gUser.getId())) {
                    throw new ThirdPartyErrorException();
                }
                return gUser.asGenericUser();
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
            bind(LinkedInAuthenticator.class)
                    .to(IAuthenticator.class)
                    .named(AuthenticatorType.LinkedIn.name())
                    .in(RequestScoped.class);
        }
    }
}
