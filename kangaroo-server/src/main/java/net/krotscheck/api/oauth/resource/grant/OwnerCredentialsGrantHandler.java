/*
 * Copyright (c) 2016 Michael Krotscheck
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
 */

package net.krotscheck.api.oauth.resource.grant;

import net.krotscheck.api.oauth.authenticator.IAuthenticator;
import net.krotscheck.api.oauth.authenticator.PasswordAuthenticator;
import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidGrantException;
import net.krotscheck.api.oauth.resource.TokenResponseEntity;
import net.krotscheck.api.oauth.util.ValidationUtil;
import net.krotscheck.features.database.entity.ApplicationScope;
import net.krotscheck.features.database.entity.Authenticator;
import net.krotscheck.features.database.entity.Client;
import net.krotscheck.features.database.entity.ClientType;
import net.krotscheck.features.database.entity.OAuthToken;
import net.krotscheck.features.database.entity.OAuthTokenType;
import net.krotscheck.features.database.entity.UserIdentity;
import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import org.apache.http.HttpStatus;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.SortedMap;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

/**
 * This grant type handler takes care of the "password" grant_type
 * OAuth flow. For situations in which a user/password is preferred, we offer
 * a simplified login mechanism, though we do not encourage its use.
 *
 * @author Michael Krotscheck
 */
public final class OwnerCredentialsGrantHandler implements IGrantTypeHandler {

    /**
     * Hibernate session, injected.
     */
    private final Session session;

    /**
     * Service locator, injected.
     */
    private final ServiceLocator locator;

    /**
     * Create a new instance of this grant handler.
     *
     * @param session Injected hibernate session.
     * @param locator The service locator.
     */
    @Inject
    public OwnerCredentialsGrantHandler(final Session session,
                                        final ServiceLocator locator) {
        this.session = session;
        this.locator = locator;
    }

    /**
     * Apply the client credentials flow to this request.
     *
     * @param client   The Client to use.
     * @param formData Raw form data for the request.
     * @return A response indicating the result of the request.
     */
    @Override
    public TokenResponseEntity handle(final Client client,
                                      final MultivaluedMap<String, String>
                                              formData) {
        // Make sure the client is the correct type.
        if (!client.getType().equals(ClientType.OwnerCredentials)) {
            throw new InvalidGrantException();
        }

        // Get the authenticator impl.
        IAuthenticator authenticator =
                locator.getService(PasswordAuthenticator.class);

        // Pull the password authenticator configuration.
        Authenticator authConfig = ValidationUtil
                .validateAuthenticator("password", client.getAuthenticators());

        // Try to resolve a user identity.
        UserIdentity identity = authenticator
                .authenticate(authConfig, formData);
        if (identity == null) {
            throw new HttpStatusException(HttpStatus.SC_UNAUTHORIZED);
        }

        // Make sure all requested scopes are in the map.
        SortedMap<String, ApplicationScope> requestedScopes =
                ValidationUtil.validateScope(formData.getFirst("scope"),
                        client.getApplication().getScopes());

        // Ensure that we retrieve a state, if it exists.
        String state = formData.getFirst("state");

        // Go ahead and create the token.
        OAuthToken token = new OAuthToken();
        token.setClient(client);
        token.setTokenType(OAuthTokenType.Bearer);
        token.setExpiresIn(client.getAccessTokenExpireIn());
        token.setScopes(requestedScopes);
        token.setIdentity(identity);

        OAuthToken refreshToken = new OAuthToken();
        refreshToken.setClient(client);
        refreshToken.setTokenType(OAuthTokenType.Refresh);
        refreshToken.setExpiresIn(client.getRefreshTokenExpireIn());
        refreshToken.setScopes(token.getScopes());
        refreshToken.setAuthToken(token);

        Transaction t = session.beginTransaction();
        session.save(token);
        session.save(refreshToken);
        t.commit();

        return TokenResponseEntity.factory(token, refreshToken, state);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(OwnerCredentialsGrantHandler.class)
                    .to(IGrantTypeHandler.class)
                    .named("password")
                    .in(RequestScoped.class);
        }
    }
}
