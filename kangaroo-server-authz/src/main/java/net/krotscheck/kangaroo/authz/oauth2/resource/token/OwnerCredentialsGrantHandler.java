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

package net.krotscheck.kangaroo.authz.oauth2.resource.token;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.password.PasswordAuthenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.common.util.ValidationUtil;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.SortedMap;

/**
 * This token type handler takes care of the "password" grant_type
 * OAuth flow. For situations in which a user/password is preferred, we offer
 * a simplified login mechanism, though we do not encourage its use.
 *
 * @author Michael Krotscheck
 */
public final class OwnerCredentialsGrantHandler
        implements ITokenRequestHandler {

    /**
     * Hibernate session, injected.
     */
    private final Session session;

    /**
     * Service locator, injected.
     */
    private final ServiceLocator locator;

    /**
     * Create a new instance of this token handler.
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
                .validateAuthenticator(AuthenticatorType.Password,
                        client.getAuthenticators());

        UserIdentity identity;
        // Try to resolve a user identity.
        identity = authenticator.authenticate(authConfig, formData);
        if (identity == null) {
            throw new NotAuthorizedException(Response.status(Status
                    .UNAUTHORIZED).build());
        }

        // Make sure all requested scopes are permitted for this user.
        SortedMap<String, ApplicationScope> requestedScopes =
                ValidationUtil.validateScope(formData.getFirst("scope"),
                        identity.getUser().getRole());

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
        refreshToken.setIdentity(identity);

        session.save(token);
        session.save(refreshToken);

        return TokenResponseEntity.factory(token, refreshToken, state);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(OwnerCredentialsGrantHandler.class)
                    .to(ITokenRequestHandler.class)
                    .named("password")
                    .in(RequestScoped.class);
        }
    }
}
