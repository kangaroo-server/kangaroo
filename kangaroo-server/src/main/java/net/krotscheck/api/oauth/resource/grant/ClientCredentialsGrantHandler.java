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

import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidGrantException;
import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.UnauthorizedClientException;
import net.krotscheck.api.oauth.resource.TokenResponseEntity;
import net.krotscheck.api.oauth.util.ValidationUtil;
import net.krotscheck.features.database.entity.ApplicationScope;
import net.krotscheck.features.database.entity.Client;
import net.krotscheck.features.database.entity.ClientType;
import net.krotscheck.features.database.entity.OAuthToken;
import net.krotscheck.features.database.entity.OAuthTokenType;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.SortedMap;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

/**
 * This grant type handler takes care of the "client_credentials" grant_type
 * OAuth flow. Its job is to provide a general-purpose access token to a
 * privileged client, such as another API server that needs to validate tokens.
 *
 * @author Michael Krotscheck
 */
public final class ClientCredentialsGrantHandler implements IGrantTypeHandler {

    /**
     * Hibernate session, injected.
     */
    private final Session session;

    /**
     * Create a new instance of this grant handler.
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
     * @param client   The Client to use.
     * @param formData Raw form data for the request.
     * @return A response indicating the result of the request.
     */
    @Override
    public TokenResponseEntity handle(final Client client,
                                      final MultivaluedMap<String, String>
                                              formData) {

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

        Transaction t = session.beginTransaction();
        session.save(token);
        t.commit();

        return TokenResponseEntity.factory(token, state);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(ClientCredentialsGrantHandler.class)
                    .to(IGrantTypeHandler.class)
                    .named("client_credentials")
                    .in(RequestScoped.class);
        }
    }
}
