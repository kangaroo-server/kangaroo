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

package net.krotscheck.api.oauth.resource;

import net.krotscheck.api.oauth.OAuthAPI;
import net.krotscheck.features.database.entity.ClientConfig;
import net.krotscheck.features.database.entity.ClientType;
import net.krotscheck.features.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.test.ContainerTest;
import net.krotscheck.test.EnvironmentBuilder;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Unit tests for the /token endpoint. Note that this is not a
 * comprehensive suite, as it only covers edge cases and functionality not
 * otherwise covered in our RFC test suites.
 *
 * @author Michael Krotscheck
 */
public final class TokenServiceTest extends ContainerTest {

    /**
     * Simple testing context.
     */
    private EnvironmentBuilder context;

    /**
     * Build and configure the application.
     *
     * @return A configured application.
     */
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new OAuthAPI();
    }

    /**
     * Set up the test harness data.
     */
    @Before
    public void createTestData() {
        context = setupEnvironment()
                .client(ClientType.ClientCredentials, true);
    }

    /**
     * Assert that an invalid grant type is rejected.
     */
    @Test
    public void testInvalidGrantType() {
        Form testData = new Form();
        testData.param("grant_type", "invalid");
        testData.param("client_id", context.getClient().getId().toString());
        testData.param("client_secret", context.getClient().getClientSecret());
        Entity testEntity = Entity.entity(testData,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        Response response = target("/token")
                .request()
                .post(testEntity);

        ErrorResponse error = response.readEntity(ErrorResponse.class);
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertEquals("invalid_grant", error.getError());
        Assert.assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that an valid grant type is accepted.
     */
    @Test
    public void testValidGrantType() {
        // Using the client credentials type here, since it was the first one
        // created.
        Form testData = new Form();
        testData.param("grant_type", "client_credentials");
        testData.param("client_id", context.getClient().getId().toString());
        testData.param("client_secret", context.getClient().getClientSecret());
        Entity testEntity = Entity.entity(testData,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        TokenResponseEntity token = target("/token")
                .request()
                .post(testEntity, TokenResponseEntity.class);

        Assert.assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        Assert.assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        Assert.assertNull(token.getRefreshToken());
        Assert.assertNull(token.getScope());
        Assert.assertNotNull(token.getAccessToken());
    }
}
