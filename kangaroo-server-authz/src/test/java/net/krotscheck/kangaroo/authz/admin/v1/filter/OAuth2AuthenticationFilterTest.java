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

package net.krotscheck.kangaroo.authz.admin.v1.filter;

import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.admin.v1.resource.AbstractResourceTest;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.entity.AbstractEntity;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;

/**
 * Tests for our authorization filter.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2AuthenticationFilterTest
        extends AbstractResourceTest {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<AbstractEntity>>
            LIST_TYPE = new GenericType<ListResponseEntity<AbstractEntity>>() {
    };

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
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<AbstractEntity>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return null;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return null;
    }

    /**
     * Provided the admin context, build a list of all additional
     * applications required for this test.
     */
    @Before
    public void setupData() {
        ApplicationContext adminContext = getAdminContext();

        // Create a new user.
        ApplicationBuilder b = adminContext.getBuilder()
                .user(null)
                .identity("remote_identity");

        // Valid token
        ApplicationContext testContext = b
                .token(OAuthTokenType.Bearer, false, Scope.USER, null, null)
                .build();
        validBearerToken = testContext.getToken();

        // Valid token, no scope.
        testContext = b
                .token(OAuthTokenType.Bearer, false, null, null, null)
                .build();
        noScopeBearerToken = testContext.getToken();

        // Expired token.
        testContext = b
                .token(OAuthTokenType.Bearer, true, Scope.USER, null, null)
                .build();
        expiredBearerToken = testContext.getToken();

        // Auth token.
        testContext = b
                .token(OAuthTokenType.Authorization, false, Scope.USER,
                        null, null)
                .build();
        authToken = testContext.getToken();
    }

    /**
     * Test a valid bearer token.
     */
    @Test
    public void testValidBearerToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(validBearerToken.getId()))
                .get();
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    /**
     * Test a valid bearer token.
     */
    @Test
    public void testNoAuthHeader() {
        Response r = target("/user")
                .request()
                .get();
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Test a valid bearer token with no scope.
     */
    @Test
    public void testValidBearerTokenWithoutScope() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(noScopeBearerToken.getId()))
                .get();
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), r.getStatus());
    }

    /**
     * Test an expired bearer token.
     */
    @Test
    public void testExpiredBearerToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(expiredBearerToken.getId()))
                .get();
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Test a bearer token that doesn't exist.
     */
    @Test
    public void testNonexistentBearerToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(
                                IdUtil.toString(IdUtil.next())))
                .get();
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Test a bearer token that isn't formatted correctly (invalid BigInteger).
     */
    @Test
    public void testMalformedBearerToken() {
        Response r = target("/user")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer YUIIUYIY")
                .get();
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
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
                        HttpUtil.authHeaderBearer(authToken.getId()))
                .get();
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
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
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
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
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return null;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractAuthzEntity entity) {
        return null;
    }
}
