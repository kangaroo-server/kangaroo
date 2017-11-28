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

import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.util.ValidationUtil;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.UnauthorizedClientException;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;

import javax.inject.Inject;
import java.util.SortedMap;

/**
 * This token type handler takes care of the "client_credentials" grant_type
 * OAuth flow. Its job is to provide a general-purpose access token to a
 * privileged client, such as another API server that needs to validate tokens.
 *
 * @author Michael Krotscheck
 */
public final class ClientCredentialsGrantHandler {

    /**
     * Hibernate session, injected.
     */
    private final Session session;

    /**
     * Create a new instance of this token handler.
     *
     * @param session Injected hibernate session.
     */
    @Inject
    public ClientCredentialsGrantHandler(final Session session) {
        this.session = session;
    }

    /**
     * Apply the client credentials flow to this request.
     *
     * @param client The Client to use.
     * @param scope  The requested scopes.
     * @param state  The state.
     * @return A response indicating the result of the request.
     */
    public TokenResponseEntity handle(final Client client,
                                      final String scope,
                                      final String state) {

        // Make sure the client is the correct type.
        if (!client.getType().equals(ClientType.ClientCredentials)) {
            throw new InvalidGrantException();
        }

        // Ensure that the client is authorized. This is actually handled in
        // the ClientAuthorizationFilter; here we check the edge case of a
        // ClientCredentials type with no set client_secret.
        if (StringUtils.isEmpty(client.getClientSecret())) {
            throw new UnauthorizedClientException();
        }

        // This flow permits requesting any of the available scopes from the
        // application, without filtering by Roles.
        SortedMap<String, ApplicationScope> requestedScopes =
                ValidationUtil.validateScope(scope,
                        client.getApplication().getScopes());

        // Go ahead and create the token.
        OAuthToken token = new OAuthToken();
        token.setClient(client);
        token.setTokenType(OAuthTokenType.Bearer);
        token.setExpiresIn(client.getAccessTokenExpireIn());
        token.setScopes(requestedScopes);

        session.save(token);

        return TokenResponseEntity.factory(token, state);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(ClientCredentialsGrantHandler.class)
                    .to(ClientCredentialsGrantHandler.class)
                    .in(RequestScoped.class);
        }
    }
}
