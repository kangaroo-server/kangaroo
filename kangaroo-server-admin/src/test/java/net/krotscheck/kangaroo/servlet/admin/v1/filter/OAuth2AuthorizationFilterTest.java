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

package net.krotscheck.kangaroo.servlet.admin.v1.filter;

import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.servlet.admin.v1.AdminV1API;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.servlet.admin.v1.filter.OAuth2AuthorizationFilter.Binder;
import net.krotscheck.kangaroo.servlet.admin.v1.filter.OAuth2AuthorizationFilter.OAuthTokenContext;
import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.apache.http.HttpStatus;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Tests for our authorization filter.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2AuthorizationFilterTest extends ContainerTest {

    /**
     * A valid, non-expired, bearer token.
     */
    private OAuthToken validBearerToken;

    /**
     * A valid, non-expired, bearer token with no appropriate scope.
     */
    private OAuthToken noScopeBearerToken;

    /**
     * An expired bearer token.
     */
    private OAuthToken expiredBearerToken;

    /**
     * A non-bearer token.
     */
    private OAuthToken authToken;

    /**
     * The application under test.
     *
     * @return An instance of the Admin servlet.
     */
    @Override
    protected ResourceConfig createApplication() {
        return new AdminV1API();
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception An exception that indicates a failed fixture load.
     */
    @Override
    public List<EnvironmentBuilder> fixtures() throws Exception {
        EnvironmentBuilder context = new EnvironmentBuilder(getSession())
                .client(ClientType.Implicit)
                .scope(Scope.USER)
                .redirect("http://example.com/redirect")
                .referrer("http://example.com/redirect")
                .authenticator("test")
                .user()
                .identity("remote_identity");

        // Valid token
        context.token(OAuthTokenType.Bearer, false, Scope.USER, null, null);
        validBearerToken = context.getToken();

        // Valid token
        context.token(OAuthTokenType.Bearer, true, null, null, null);
        noScopeBearerToken = context.getToken();

        // Expired token.
        context.token(OAuthTokenType.Bearer, true, Scope.USER, null, null);
        expiredBearerToken = context.getToken();

        // Auth token
        context.authToken();
        authToken = context.getToken();

        List<EnvironmentBuilder> fixtures = new ArrayList<>();
        fixtures.add(context);
        return fixtures;
    }

    /**
     * Test a valid bearer token.
     */
    @Test
    public void testValidBearerToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        String.format("Bearer %s", validBearerToken.getId()))
                .get();
        Assert.assertEquals(HttpStatus.SC_OK, r.getStatus());
    }

    /**
     * Test a valid bearer token.
     */
    @Test
    public void testNoAuthHeader() {
        Response r = target("/user")
                .request()
                .get();
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
    }

    /**
     * Test a valid bearer token with no scope.
     */
    @Test
    public void testValidBearerTokenWithoutScope() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        String.format("Bearer %s", noScopeBearerToken.getId()))
                .get();
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
    }

    /**
     * Test an expired bearer token.
     */
    @Test
    public void testExpiredBearerToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        String.format("Bearer %s", expiredBearerToken.getId()))
                .get();
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
    }

    /**
     * Test a bearer token that doesn't exist.
     */
    @Test
    public void testNonexistentBearerToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        String.format("Bearer %s", UUID.randomUUID()))
                .get();
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
    }

    /**
     * Test a bearer token that isn't formatted correctly (invalid UUID).
     */
    @Test
    public void testMalformedBearerToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer YUIIUYIY")
                .get();
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
    }

    /**
     * Test a token that isn't actually a bearer token, such as an
     * authorization token.
     */
    @Test
    public void testAuthorizationToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        String.format("Bearer %s", authToken.getId()))
                .get();
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
    }

    /**
     * Test a token with the wrong prefix type.
     */
    @Test
    public void testWrongPrefixTokenToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        String.format("HMAC %s", authToken.getId()))
                .get();
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
    }

    /**
     * Test a token with no bearer prefix.
     */
    @Test
    public void testMalformedToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "OMGOMGOMG")
                .get();
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
    }

    /**
     * Assert that we can invoke the binder.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testBinder() throws Exception {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocator locator = factory.create(getClass().getCanonicalName());

        Binder b = new OAuth2AuthorizationFilter.Binder();
        ServiceLocatorUtilities.bind(locator, b);

        List<ActiveDescriptor<?>> descriptors =
                locator.getDescriptors(
                        BuilderHelper.createContractFilter(
                                ContainerRequestFilter.class.getName()));
        Assert.assertEquals(1, descriptors.size());

        ActiveDescriptor descriptor = descriptors.get(0);
        Assert.assertNotNull(descriptor);
        // Check scope...
        Assert.assertEquals(Singleton.class.getCanonicalName(),
                descriptor.getScope());

        // ... check name.
        Assert.assertNull(descriptor.getName());
    }

    /**
     * Assert that we can invoke the binder.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testContextMethods() throws Exception {
        OAuthTokenContext context =
                new OAuthTokenContext(new OAuthToken(), false);

        Assert.assertFalse(context.isSecure());
        Assert.assertEquals("OAuth2", context.getAuthenticationScheme());
    }
}
