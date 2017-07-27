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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.oauth2.OAuthAPI;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.test.jerseyTest.ContainerTest;
import net.krotscheck.kangaroo.test.HttpUtil;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.UUID;

/**
 * Unit tests for the authorization endpoint. Note that this is not a
 * comprehensive suite, as it only covers edge cases and functionality not
 * otherwise covered in our RFC test suites.
 *
 * @author Michael Krotscheck
 */
public final class AuthorizationServiceTest extends ContainerTest {

    /**
     * Test data loading for this test.
     */
    @ClassRule
    public static final TestRule TEST_DATA_RULE =
            new TestDataResource(HIBERNATE_RESOURCE) {
                /**
                 * Initialize the test data.
                 */
                @Override
                protected void loadTestData(final Session session) {
                    context = ApplicationBuilder.newApplication(session)
                            .client(ClientType.Implicit)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .build();

                    ownerContext = ApplicationBuilder.newApplication(session)
                            .client(ClientType.OwnerCredentials)
                            .authenticator(AuthenticatorType.Test)
                            .redirect("http://valid.example.com/redirect")
                            .authenticatorState()
                            .build();
                }
            };

    /**
     * Simple testing context.
     */
    private static ApplicationContext context;

    /**
     * An owner context.
     */
    private static ApplicationContext ownerContext;

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
     * Assert that an invalid grant type is rejected.
     */
    @Test
    public void testInvalidResponseType() {
        Response r = target("/authorize")
                .queryParam("response_type", "invalid")
                .queryParam("client_id", context.getClient().getId())
                .request()
                .get();

        Assert.assertEquals(Status.FOUND.getStatusCode(), r.getStatus());

        URI location = r.getLocation();
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertEquals("unsupported_response_type",
                params.getFirst("error"));
        Assert.assertNotNull(params.get("error_description"));
    }

    /**
     * Assert that an valid grant type is accepted.
     */
    @Test
    public void testValidResponseType() {
        Response r = target("/authorize")
                .queryParam("response_type", "test")
                .queryParam("client_id", context.getClient().getId())
                .request()
                .get();

        URI location = r.getLocation();
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getFragment());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertEquals("unsupported_response_type",
                params.getFirst("error"));
        Assert.assertNotNull(params.get("error_description"));
    }

    /**
     * Assert that an valid grant type is accepted.
     */
    @Test
    public void testCallbackMalformedStateId() {
        Response r = target("/authorize/callback")
                .queryParam("state", "not_a_parseable_uuid")
                .request()
                .get();

        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        ErrorResponse e = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", e.getError());
        Assert.assertNotNull(e.getErrorDescription());
    }

    /**
     * Assert that an valid grant type is accepted.
     */
    @Test
    public void testCallbackInvalidStateId() {
        Response r = target("/authorize/callback")
                .queryParam("state", UUID.randomUUID().toString())
                .request()
                .get();

        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        ErrorResponse e = r.readEntity(ErrorResponse.class);
        Assert.assertEquals("invalid_request", e.getError());
        Assert.assertNotNull(e.getErrorDescription());
    }

    /**
     * If, for some inexplicable reason, an AuthenticatorState was created
     * linked to a non-implicit or authorization-grant client, we should make
     * sure that the request errors.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testCallbackStateWithInvalidClientType() throws Exception {
        Response r = target("/authorize/callback")
                .queryParam("state", ownerContext.getAuthenticatorState()
                        .getId().toString())
                .request()
                .get();

        URI location = r.getLocation();
        MultivaluedMap<String, String> params =
                HttpUtil.parseQueryParams(location.getQuery());
        Assert.assertEquals("valid.example.com", location.getHost());
        Assert.assertEquals("/redirect", location.getPath());
        Assert.assertEquals("invalid_request", params.getFirst("error"));
        Assert.assertNotNull(params.get("error_description"));
    }
}
