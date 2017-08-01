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

package net.krotscheck.kangaroo.authz.admin.v1.auth.filter;

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.authz.admin.v1.auth.OAuth2SecurityContext;
import net.krotscheck.kangaroo.authz.admin.v1.auth.exception.OAuth2NotAuthorizedException;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.Config;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.HttpUtil;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the AuthenticationFilter.
 *
 * @author Michael Krotscheck
 */
public class OAuth2AuthenticationFilterTest extends DatabaseTest {

    /**
     * The application context from which we'll build our test scenarios.
     */
    private static ApplicationContext context;

    /**
     * Preload data into the system.
     */
    @ClassRule
    public static final TestDataResource TEST_DATA_RESOURCE =
            new TestDataResource(HIBERNATE_RESOURCE) {
                @Override
                protected void loadTestData(final Session session) {
                    context = ApplicationBuilder.newApplication(session)
                            .role("admin")
                            .scope("one")
                            .scope("two")
                            .client(ClientType.OwnerCredentials)
                            .authenticator(AuthenticatorType.Test)
                            .user()
                            .build();
                }
            };
    /**
     * Session provider, used in tests.
     */
    private final Provider<Session> sessionProvider = this::getSession;

    /**
     * The request's servlet configuration provider.
     */
    private final Provider<Configuration> configProvider = () -> {
        Map<String, Object> config = new HashMap<>();
        config.put(Config.APPLICATION_ID,
                context.getApplication().getId().toString());
        return new MapConfiguration(config);
    };

    /**
     * Request context.
     */
    private ContainerRequestContext requestContext;

    /**
     * Security context.
     */
    private SecurityContext securityContext;

    /**
     * Assert that this filter has the Authentication priority.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertCorrectPriority() throws Exception {
        Priority a = OAuth2AuthenticationFilter.class
                .getAnnotation(Priority.class);
        Assert.assertEquals(Priorities.AUTHENTICATION, a.value());
    }

    /**
     * Setup of common mocks.
     */
    @Before
    public void setup() {
        requestContext = mock(ContainerRequestContext.class);
        securityContext = mock(SecurityContext.class);
        UriInfo mockInfo = mock(UriInfo.class);
        UriBuilder mockBuilder = UriBuilder.fromPath("http://example.com/");
        List<String> matchedPaths = Collections.singletonList("path");

        doReturn(securityContext).when(requestContext).getSecurityContext();
        doReturn(mockInfo).when(requestContext).getUriInfo();
        doReturn(mockBuilder).when(mockInfo).getBaseUriBuilder();
        doReturn(matchedPaths).when(mockInfo).getMatchedURIs();
    }

    /**
     * Assert that a valid bearer token resolves.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertValidToken() throws Exception {
        OAuthToken token = context.getBuilder()
                .bearerToken("one")
                .build()
                .getToken();

        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        String header = HttpUtil.authHeaderBearer(token.getId());
        doReturn(header).when(requestContext)
                .getHeaderString(HttpHeaders.AUTHORIZATION);

        filter.filter(requestContext);

        ArgumentCaptor<OAuth2SecurityContext> contextCaptor =
                ArgumentCaptor.forClass(OAuth2SecurityContext.class);
        verify(requestContext, times(1))
                .setSecurityContext(contextCaptor.capture());

        Assert.assertEquals(token, contextCaptor.getValue().getUserPrincipal());
    }

    /**
     * Assert that running without a security context throws.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2NotAuthorizedException.class)
    public void assertNoContext() throws Exception {
        doReturn(null).when(requestContext).getSecurityContext();

        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        filter.filter(requestContext);
    }

    /**
     * Assert that a null token exists.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2NotAuthorizedException.class)
    public void assertEmptyToken() throws Exception {
        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        String header = HttpUtil.authHeaderBearer("");
        doReturn(header).when(requestContext)
                .getHeaderString(HttpHeaders.AUTHORIZATION);

        filter.filter(requestContext);
    }

    /**
     * Assert that a null token exists.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2NotAuthorizedException.class)
    public void assertInvalidToken() throws Exception {
        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        String header = HttpUtil.authHeaderBearer(UUID.randomUUID());
        doReturn(header).when(requestContext)
                .getHeaderString(HttpHeaders.AUTHORIZATION);

        filter.filter(requestContext);
    }

    /**
     * Assert that an expired token exists.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2NotAuthorizedException.class)
    public void assertExpiredToken() throws Exception {
        OAuthToken token = context.getBuilder()
                .token(OAuthTokenType.Bearer, true, "one", null, null)
                .build()
                .getToken();

        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        String header = HttpUtil.authHeaderBearer(token.getId());
        doReturn(header).when(requestContext)
                .getHeaderString(HttpHeaders.AUTHORIZATION);

        filter.filter(requestContext);
    }

    /**
     * Assert that an empty header fails.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2NotAuthorizedException.class)
    public void assertEmptyHeader() throws Exception {
        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        doReturn("").when(requestContext)
                .getHeaderString(HttpHeaders.AUTHORIZATION);

        filter.filter(requestContext);
    }

    /**
     * Assert that a header with an invalid format fails.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2NotAuthorizedException.class)
    public void assertBadlyFormattedHeader() throws Exception {
        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        doReturn("invalid_format").when(requestContext)
                .getHeaderString(HttpHeaders.AUTHORIZATION);

        filter.filter(requestContext);
    }

    /**
     * Assert that a non-bearer token header fails.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2NotAuthorizedException.class)
    public void assertNoBearerHeader() throws Exception {
        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        doReturn("HMAC Token").when(requestContext)
                .getHeaderString(HttpHeaders.AUTHORIZATION);

        filter.filter(requestContext);
    }

    /**
     * Assert that a non-UUID bearer header value fails.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2NotAuthorizedException.class)
    public void assertBadlyFormedToken() throws Exception {
        OAuth2AuthenticationFilter filter = new OAuth2AuthenticationFilter(
                sessionProvider, configProvider, new String[]{"one"});

        doReturn("Bearer not_a_uuid").when(requestContext)
                .getHeaderString(HttpHeaders.AUTHORIZATION);

        filter.filter(requestContext);
    }
}
