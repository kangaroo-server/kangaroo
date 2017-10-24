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

package net.krotscheck.kangaroo.authz.common.authenticator.google;

import com.google.common.base.Strings;
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

/**
 * This authentication helper permits using google as an IdP. It's not a
 * true google api client; it immediately discards any issued token, and
 * does no subsequent lookups on the user. In other words, this will not
 * provide you with fancy amazing google features, though we may choose to
 * enable this in the future.
 *
 * @author Michael Krotscheck
 */
public final class GoogleAuthenticator
        extends AbstractOAuth2Authenticator {

    /**
     * Google's auth endpoint.
     *
     * @return The absolute URL to google's auth endpoint.
     */
    @Override
    protected String getAuthEndpoint() {
        return "https://accounts.google.com/o/oauth2/v2/auth";
    }

    /**
     * Google's token endpoint.
     *
     * @return The absolute URL to google's token endpoint.
     */
    @Override
    protected String getTokenEndpoint() {
        return "https://www.googleapis.com/oauth2/v4/token";
    }

    /**
     * List of scopes that we need from google.
     *
     * @return A static list of scopes.
     */
    @Override
    protected String getScopes() {
        return StringUtils.join(new String[]{
                "https://www.googleapis.com/auth/userinfo.email",
                "https://www.googleapis.com/auth/userinfo.profile"
        }, " ");
    }

    /**
     * Retrieve the user info from Google.
     *
     * @param token The OAuth token.
     * @return The remote user data from Google.
     */
    @Override
    protected OAuth2User loadUserIdentity(final OAuth2IdPToken token) {
        Response r = getClient()
                .target("https://www.googleapis.com/oauth2/v1/userinfo")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getAccessToken()))
                .get();
        try {
            // If this is an error...
            if (r.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                GoogleUserEntity gUser =
                        r.readEntity(GoogleUserEntity.class);
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
            bind(GoogleAuthenticator.class)
                    .to(IAuthenticator.class)
                    .named(AuthenticatorType.Google.name())
                    .in(RequestScoped.class);
        }
    }
}
