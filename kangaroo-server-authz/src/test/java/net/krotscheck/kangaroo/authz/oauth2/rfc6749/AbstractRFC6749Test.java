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

package net.krotscheck.kangaroo.authz.oauth2.rfc6749;

import net.krotscheck.kangaroo.authz.common.database.entity.ClientConfig;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.OAuthAPI;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import net.krotscheck.kangaroo.test.jersey.SingletonTestContainerFactory;
import net.krotscheck.kangaroo.test.runner.SingleInstanceTestRunner;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MultivaluedMap;
import java.math.BigInteger;


/**
 * Abstract testing class that bootstraps a full OAuthAPI that's ready for
 * external hammering.
 *
 * @author Michael Krotscheck
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-2.3">https://tools.ietf.org/html/rfc6749#section-2.3</a>
 */
@RunWith(SingleInstanceTestRunner.class)
public abstract class AbstractRFC6749Test extends ContainerTest {

    /**
     * Test container factory.
     */
    private SingletonTestContainerFactory testContainerFactory;
    /**
     * The current running test application.
     */
    private ResourceConfig testApplication;

    /**
     * This method asserts that a token has been persisted to the database,
     * and contains a brief set of expected variables.
     *
     * @param params          URL Parameters.
     * @param requireIdentity Whether a User Identity is required.
     */
    protected void assertValidBearerToken(
            final MultivaluedMap<String, String> params,
            final boolean requireIdentity) {
        Session s = getSession();

        String accessTokenString = params.getFirst("access_token");
        String tokenType = params.getFirst("token_type");
        String state = params.getFirst("state");
        Long expiresIn = Long.valueOf(params.getFirst("expires_in"));
        BigInteger accessTokenId = IdUtil.fromString(accessTokenString);

        OAuthToken t = s.get(OAuthToken.class, accessTokenId);
        Assert.assertEquals(OAuthTokenType.valueOf(tokenType),
                t.getTokenType());
        Assert.assertEquals(expiresIn, t.getExpiresIn());

        TokenResponseEntity entity = TokenResponseEntity.factory(t, state);

        assertValidBearerToken(entity, requireIdentity);
    }

    /**
     * This method asserts that a token has been persisted to the database,
     * and contains a brief set of expected variables.
     *
     * @param token           The token.
     * @param requireIdentity Whether a User Identity is required.
     */
    protected void assertValidBearerToken(final TokenResponseEntity token,
                                          final boolean requireIdentity) {
        Session s = getSession();

        // Validate the token itself.
        Assert.assertNotNull(token.getAccessToken());
        Assert.assertEquals(
                Long.valueOf(ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT),
                token.getExpiresIn());
        Assert.assertEquals(OAuthTokenType.Bearer, token.getTokenType());

        OAuthToken bearer = s.get(OAuthToken.class, token.getAccessToken());
        Assert.assertNotNull(bearer);
        Assert.assertEquals(
                Long.valueOf(ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT),
                bearer.getExpiresIn());
        Assert.assertEquals(bearer.getTokenType(), OAuthTokenType.Bearer);

        // Do we expect an identity?
        if (requireIdentity) {
            Assert.assertNotNull(bearer.getIdentity());
        }

        // Validate any attached refresh token.
        BigInteger refreshTokenId = token.getRefreshToken();
        if (refreshTokenId != null) {
            OAuthToken refresh = s.get(OAuthToken.class, refreshTokenId);
            Assert.assertEquals(refresh.getAuthToken(), bearer);
            Assert.assertEquals(refresh.getIdentity(), bearer.getIdentity());
            Assert.assertEquals(refresh.getScopes(), bearer.getScopes());
            Assert.assertEquals(refresh.getTokenType(), OAuthTokenType.Refresh);
            Assert.assertEquals(refresh.getClient(), bearer.getClient());
            Assert.assertEquals(
                    Long.valueOf(ClientConfig.REFRESH_TOKEN_EXPIRES_DEFAULT),
                    refresh.getExpiresIn());
        }
    }

    /**
     * This method overrides the underlying default test container provider,
     * with one that provides a singleton instance. This allows us to
     * circumvent the often expensive initialization routines that come from
     * bootstrapping our services.
     *
     * @return an instance of {@link TestContainerFactory} class.
     * @throws TestContainerException if the initialization of
     *                                {@link TestContainerFactory} instance
     *                                is not successful.
     */
    protected TestContainerFactory getTestContainerFactory()
            throws TestContainerException {
        if (this.testContainerFactory == null) {
            this.testContainerFactory =
                    new SingletonTestContainerFactory(
                            super.getTestContainerFactory(),
                            this.getClass());
        }
        return testContainerFactory;
    }

    /**
     * Create the application under test.
     *
     * @return A configured api servlet.
     */
    @Override
    protected final ResourceConfig createApplication() {
        if (testApplication == null) {
            testApplication = new OAuthAPI();
        }
        return testApplication;
    }
}
