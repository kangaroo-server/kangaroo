/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.oauth2.authn.authn;

import net.krotscheck.kangaroo.authz.admin.v1.servlet.FirstRunContainerLifecycleListener;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.authz.admin.v1.test.rule.TestDataResource;
import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2AuthDynamicFeature;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.math.BigInteger;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static net.krotscheck.kangaroo.util.HttpUtil.authHeaderBearer;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for Bearer-token based authentication.
 *
 * @author Michael Krotscheck
 */
public final class O2BearerTokenFilterTest extends ContainerTest {

    /**
     * Preload data into the system.
     */
    @ClassRule
    public static final TestDataResource TEST_DATA_RESOURCE =
            new TestDataResource(HIBERNATE_RESOURCE);

    /**
     * Setup an application.
     *
     * @return A configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();

        // Build a minimal application
        a.register(ConfigurationFeature.class);
        a.register(DatabaseFeature.class);
        a.register(ExceptionFeature.class);
        a.register(new ServletConfigFactory.Binder());
        a.register(new FirstRunContainerLifecycleListener.Binder());

        // Layer in the code under test. Taking the whole feature set here,
        // because it's easier.
        a.register(O2AuthDynamicFeature.class);

        // Add our test resource.
        a.register(O2TestResource.class);

        return a;
    }

    /**
     * Assert that a request with no authorization header fails.
     */
    @Test
    public void testNoAuthorization() {
        Response r = target("/token")
                .request()
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with an invalid header fails.
     */
    @Test
    public void testInvalidAuthorizationHeader() {
        Response r = target("/token")
                .request()
                .header(AUTHORIZATION, "Not A Valid header")
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a malformed Bearer header fails.
     */
    @Test
    public void testInvalidBearerHeader() {
        Response r = target("/token")
                .request()
                .header(AUTHORIZATION, "Bearer some_secluded_rendezvous")
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a malformed user/pass indicates a bad request.
     */
    @Test
    public void testMalformedCredentialsHeader() {
        String header =
                authHeaderBearer("malformed_token");

        Response r = target("/token")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with nonexistent credentials fails.
     */
    @Test
    public void testNonexistentCredentialsHeader() {
        BigInteger id = IdUtil.next();

        String header = authHeaderBearer(id);

        Response r = target("/token")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID of a private client, but invalid
     * secret fails.
     */
    @Test
    public void testBadPasswordHeader() {
        ApplicationContext context =
                TEST_DATA_RESOURCE.getSecondaryApplication().getBuilder()
                        .client(ClientType.AuthorizationGrant, true)
                        .build();
        Client c = context.getClient();

        String header = authHeaderBearer(c.getId());

        Response r = target("/token")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID of a public client, and no
     * secret fails.
     */
    @Test
    public void testPublicClientHeader() {
        ApplicationContext context =
                TEST_DATA_RESOURCE.getSecondaryApplication().getBuilder()
                        .client(ClientType.AuthorizationGrant, false)
                        .build();
        Client c = context.getClient();

        String header = authHeaderBearer(c.getId());

        Response r = target("/token")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a valid bearer token for a private client passes.
     */
    @Test
    public void testValidRequestHeaderPrivate() {
        ApplicationContext context =
                TEST_DATA_RESOURCE
                        .getSecondaryApplication()
                        .getBuilder()
                        .client(ClientType.AuthorizationGrant, true)
                        .bearerToken()
                        .build();
        OAuthToken t = context.getToken();

        String header = authHeaderBearer(t.getId());

        Response r = target("/token/private")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a valid bearer token for a private client fails if not
     * allowed.
     */
    @Test
    public void testValidRequestHeaderPrivateNotPermitted() {
        ApplicationContext context =
                TEST_DATA_RESOURCE
                        .getSecondaryApplication()
                        .getBuilder()
                        .client(ClientType.AuthorizationGrant, true)
                        .bearerToken()
                        .build();
        OAuthToken t = context.getToken();

        String header = authHeaderBearer(t.getId());

        Response r = target("/token/public")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a bearer token for a public client passes if permitted.
     */
    @Test
    public void testValidRequestHeaderPublic() {
        ApplicationContext context =
                TEST_DATA_RESOURCE
                        .getSecondaryApplication()
                        .getBuilder()
                        .client(ClientType.AuthorizationGrant, false)
                        .bearerToken()
                        .build();
        OAuthToken t = context.getToken();

        String header = authHeaderBearer(t.getId());

        Response r = target("/token/public")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a bearer token for a public client fails if not permitted.
     */
    @Test
    public void testValidRequestHeaderPublicNotPermitted() {
        ApplicationContext context =
                TEST_DATA_RESOURCE
                        .getSecondaryApplication()
                        .getBuilder()
                        .client(ClientType.Implicit, false)
                        .bearerToken()
                        .build();
        OAuthToken t = context.getToken();

        String header = authHeaderBearer(t.getId());

        Response r = target("/token/private")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that an expired token fails.
     */
    @Test
    public void testExpiredToken() {
        ApplicationContext context =
                TEST_DATA_RESOURCE
                        .getSecondaryApplication()
                        .getBuilder()
                        .client(ClientType.AuthorizationGrant, true)
                        .token(OAuthTokenType.Bearer, true, null, null, null)
                        .build();
        OAuthToken t = context.getToken();

        String header = authHeaderBearer(t.getId());

        Response r = target("/token/private")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that refresh tokens are not permitted.
     */
    @Test
    public void testRefreshToken() {
        ApplicationContext context =
                TEST_DATA_RESOURCE
                        .getSecondaryApplication()
                        .getBuilder()
                        .client(ClientType.AuthorizationGrant, true)
                        .bearerToken()
                        .refreshToken()
                        .build();
        OAuthToken t = context.getToken();

        String header = authHeaderBearer(t.getId());

        Response r = target("/token/private")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that authorization codes are not permitted.
     */
    @Test
    public void testAuthorizationCode() {
        ApplicationContext context =
                TEST_DATA_RESOURCE
                        .getSecondaryApplication()
                        .getBuilder()
                        .client(ClientType.AuthorizationGrant, true)
                        .authToken()
                        .build();
        OAuthToken t = context.getToken();

        String header = authHeaderBearer(t.getId());

        Response r = target("/token/private")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }
}
