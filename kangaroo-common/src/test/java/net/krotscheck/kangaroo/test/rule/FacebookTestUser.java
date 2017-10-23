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

package net.krotscheck.kangaroo.test.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.krotscheck.kangaroo.common.httpClient.JerseyClientBuilderFactory;
import net.krotscheck.kangaroo.test.TestConfig;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * This JUnit4 rule creates (and deletes) a test facebook user.
 *
 * @author Michael Krotscheck
 */
public final class FacebookTestUser implements TestRule {

    /**
     * The App Id.
     */
    private final String appId = TestConfig.getFacebookAppId();

    /**
     * The App Secret.
     */
    private final String appSecret = TestConfig.getFacebookAppSecret();

    /**
     * The created test user.
     */
    private FBTestUser testUser;

    /**
     * Create a new facebook test user rule.
     */
    public FacebookTestUser() {
    }

    /**
     * Get the user email.
     *
     * @return The user email.
     */
    public String getEmail() {
        return testUser.email;
    }

    /**
     * Get the user password.
     *
     * @return The user password.
     */
    public String getPassword() {
        return testUser.password;
    }

    /**
     * Get the user's login url.
     *
     * @return The login url (expires in one hour).
     */
    public String getLoginUrl() {
        return testUser.loginUrl;
    }

    /**
     * Start the rule.
     *
     * @param base
     * @param description
     * @return
     */
    @Override
    public Statement apply(final Statement base,
                           final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                Client client = null;
                try {
                    client = new JerseyClientBuilderFactory()
                            .get().build();
                    FBAuthToken token = getAppToken(client);
                    try {
                        createFacebookUser(client, token);
                        base.evaluate();
                    } finally {
                        deleteFacebookUser(client, token);
                    }
                } finally {
                    if (client != null) {
                        client.close();
                    }
                }
            }
        };
    }

    /**
     * Use facebook's graph api to create a user.
     *
     * @param client   The HTTP client to use.
     * @param appToken The App's API Token
     */
    private void createFacebookUser(final Client client,
                                    final FBAuthToken appToken) {
        URI target = UriBuilder.fromUri("https://graph.facebook.com")
                .path(String.format("/v2.10/%s/accounts/test-users", appId))
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("installed", "true");
        params.put("permissions", "public_profile,email");

        Entity<Map<String, String>> postEntity = Entity.entity(params,
                MediaType.APPLICATION_JSON_TYPE);

        Response r = client.target(target)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(appToken.accessToken))
                .post(postEntity);

        testUser = r.readEntity(FBTestUser.class);
        r.close();
    }

    /**
     * User facebook's graph api to delete the test user.
     *
     * @param client   The HTTP client to use.
     * @param appToken The App's API Token
     */
    private void deleteFacebookUser(final Client client,
                                    final FBAuthToken appToken) {
        URI target = UriBuilder.fromUri("https://graph.facebook.com")
                .path(String.format("/v2.10/%s", testUser.id))
                .build();

        Response r = client.target(target)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(appToken.accessToken))
                .delete();
        testUser = null;
        r.close();
    }

    /**
     * Get an api token.
     *
     * @param client The HTTP client to use.
     * @return An API token for the configured application.
     */
    private FBAuthToken getAppToken(final Client client) {
        URI target = UriBuilder.fromUri("https://graph.facebook.com")
                .path("/oauth/access_token")
                .build();

        Response r = client.target(target)
                .queryParam("client_id", appId)
                .queryParam("client_secret", appSecret)
                .queryParam("grant_type", "client_credentials")
                .request()
                .get();

        FBAuthToken token = r.readEntity(FBAuthToken.class);
        r.close();
        return token;
    }

    /**
     * Facebook auth token.
     */
    private static final class FBAuthToken {

        /**
         * The access token.
         */
        @JsonProperty("access_token")
        private String accessToken;

        /**
         * The token type (bearer or authorization).
         */
        @JsonProperty("token_type")
        private String tokenType;
    }

    /**
     * Facebook test user entity.
     */
    private static final class FBTestUser {

        /**
         * The user's id.
         */
        @JsonProperty("id")
        private String id;

        /**
         * The access token belonging to the test user.
         */
        @JsonProperty("access_token")
        private String accessToken;

        /**
         * A login url that may be used for this test user.
         */
        @JsonProperty("login_url")
        private String loginUrl;

        /**
         * The user's new email address.
         */
        @JsonProperty("email")
        private String email;

        /**
         * The user's password.
         */
        @JsonProperty("password")
        private String password;
    }
}
