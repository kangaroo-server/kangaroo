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

package net.krotscheck.kangaroo.servlet.oauth2.resource;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.database.entity.ClientConfig;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.servlet.oauth2.OAuthAPI;
import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Entity;
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
    protected ResourceConfig createApplication() {
        return new OAuthAPI();
    }

    /**
     * Load data fixtures for each test.
     *
     * @param session The session to use to build the environment.
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<EnvironmentBuilder> fixtures(final Session session) {
        context = new EnvironmentBuilder(session)
                .client(ClientType.ClientCredentials, true);

        List<EnvironmentBuilder> fixtures = new ArrayList<>();
        fixtures.add(context);
        return fixtures;
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
