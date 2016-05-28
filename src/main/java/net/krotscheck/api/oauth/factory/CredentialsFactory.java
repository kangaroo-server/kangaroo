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

package net.krotscheck.api.oauth.factory;

import net.krotscheck.api.oauth.factory.CredentialsFactory.Credentials;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ContainerRequest;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * This factory attempts to determine the current client credentials
 * (client_id and client_secret) from the current context. If a request is
 * not available, it will return null.
 *
 * @author Michael Krotscheck
 */
public final class CredentialsFactory implements Factory<Credentials> {

    /**
     * Convenient empty credential instance.
     */
    public static final Credentials EMPTY = new Credentials();

    /**
     * The request provider.
     */
    private final Provider<ContainerRequest> requestProvider;

    /**
     * Create a new instance of the credential factory.
     *
     * @param requestProvider The container request provider, injected by HK2.
     */
    @Inject
    public CredentialsFactory(final Provider<ContainerRequest>
                                      requestProvider) {
        this.requestProvider = requestProvider;
    }

    /**
     * Resolve the credentials from the provided request context.
     *
     * @return The produces object
     */
    @Override
    public Credentials provide() {
        ContainerRequest request = requestProvider.get();
        Credentials headerCreds;
        Credentials requestCreds;
        try {
            headerCreds = resolveHeaderCredentials(request);
            requestCreds = resolveRequestCredentials(request);
        } catch (Exception e) {
            return EMPTY;
        }

        // If we don't have a clientId or requestCreds, fail.
        if (requestCreds == null || requestCreds.getLogin() == null) {
            return EMPTY;
        }

        // If we have a secret in the GET headers, exit.
        if (request.getMethod().equals(HttpMethod.GET)
                && !StringUtils.isEmpty(requestCreds.getPassword())) {
            return EMPTY;
        }

        // If we have header credentials...
        if (headerCreds != null) {

            // ... the ID's must match.
            if (!requestCreds.getLogin().equals(headerCreds.getLogin())) {
                return EMPTY;
            }

            // ...the header must have a password.
            if (StringUtils.isEmpty(headerCreds.getPassword())) {
                return EMPTY;
            }

            // ... the request credentials may NOT have a password.
            if (!StringUtils.isEmpty(requestCreds.getPassword())) {
                return EMPTY;
            }

            return headerCreds;
        }

        return requestCreds;
    }

    /**
     * Build the header credentials.
     *
     * @param request The container request to extract the value from.
     * @return Credentials resolved from the request headers.
     */
    private Credentials resolveHeaderCredentials(
            final ContainerRequest request) {
        // Build up the header credentials
        String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Do we have a header?
        if (header != null) {
            if (header.startsWith("Basic")) {
                String base64Credentials = header.substring("Basic".length())
                        .trim();
                String credentials = new String(
                        Base64.getDecoder().decode(base64Credentials),
                        Charset.forName("UTF-8"));
                String[] values = credentials.split(":", 2);
                return new Credentials(values[0], values[1]);
            } else {
                return EMPTY;
            }
        }
        return null;
    }

    /**
     * Build the request credentials.
     *
     * @param request The container request to extract the value from.
     * @return Credentials resolved from the request's body.
     */
    private Credentials resolveRequestCredentials(
            final ContainerRequest request) {
        String method = request.getMethod();
        if (method.equals(HttpMethod.POST)) {
            // Buffer the entity.
            request.bufferEntity();
            return new Credentials(request.readEntity(Form.class));
        } else if (method.equals(HttpMethod.GET)) {
            // ... or from the GET query string.
            List<NameValuePair> params = URLEncodedUtils.parse(
                    request.getRequestUri(), "UTF-8");
            String clientId = null;
            String clientSecret = null;
            for (NameValuePair pair : params) {
                String name = pair.getName();
                if (name.equals("client_id")) {
                    clientId = pair.getValue();
                } else if (name.equals("client_secret")) {
                    clientSecret = pair.getValue();
                }
            }
            return new Credentials(clientId, clientSecret);
        }
        return null;
    }

    /**
     * Credentials are static pojo's, they can just be dereferenced.
     *
     * @param instance The credentials to dispose of.
     */
    @Override
    public void dispose(final Credentials instance) {
        // Do nothing.
    }

    /**
     * Client credentials for the current request.
     */
    public static final class Credentials {

        /**
         * The login.
         */
        private UUID login = null;

        /**
         * The password.
         */
        private String password = null;

        /**
         * Create a new credentials instance.
         */
        public Credentials() {
        }

        /**
         * Create a new credentials instance.
         *
         * @param login    The login.
         * @param password The password.
         */
        public Credentials(final String login, final String password) {
            this.login = UUID.fromString(login);

            if (StringUtils.isEmpty(password)) {
                this.password = null;
            } else {
                this.password = password;
            }
        }

        /**
         * Create a new credentials instance.
         *
         * @param formData Form data to parse for credentials.
         */
        public Credentials(final Form formData) {
            MultivaluedMap<String, String> data = formData.asMap();
            if (data.containsKey("client_id")) {
                login = UUID.fromString(data.getFirst("client_id"));
            } else {
                login = null;
            }

            if (data.containsKey("client_secret")
                    && !StringUtils.isEmpty(data.getFirst("client_secret"))) {
                password = data.getFirst("client_secret");
            } else {
                password = null;
            }
        }

        /**
         * Get the login.
         *
         * @return The login.
         */
        public UUID getLogin() {
            return login;
        }

        /**
         * Get the password.
         *
         * @return The password.
         */
        public String getPassword() {
            return password;
        }

        /**
         * Is this set of credentials valid?
         *
         * @return True if it has an ID, otherwise false.
         */
        public Boolean isValid() {
            return login != null;
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(CredentialsFactory.class)
                    .to(Credentials.class)
                    .in(Singleton.class);
        }
    }
}
