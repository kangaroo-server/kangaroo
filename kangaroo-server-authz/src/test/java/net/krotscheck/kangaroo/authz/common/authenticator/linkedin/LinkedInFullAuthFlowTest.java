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

package net.krotscheck.kangaroo.authz.common.authenticator.linkedin;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.AbstractOAuth2Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.oauth2.OAuthAPI;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.TestConfig;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import net.krotscheck.kangaroo.test.jersey.SingletonTestContainerFactory;
import net.krotscheck.kangaroo.test.rule.SeleniumRule;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import net.krotscheck.kangaroo.test.runner.SingleInstanceTestRunner;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * This test performs a series of full login flows against facebook.
 *
 * @author Michael Krotscheck
 */
@RunWith(SingleInstanceTestRunner.class)
public final class LinkedInFullAuthFlowTest extends ContainerTest {

    /**
     * The selenium rule.
     */
    @ClassRule
    public static final SeleniumRule SELENIUM = new SeleniumRule();

    /**
     * The test context for a regular application.
     */
    private static ApplicationContext context;

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
                    Map<String, String> config = new HashMap<>();
                    config.put(AbstractOAuth2Authenticator.CLIENT_ID_KEY,
                            TestConfig.getLinkedInAppId());
                    config.put(AbstractOAuth2Authenticator.CLIENT_SECRET_KEY,
                            TestConfig.getLinkedInAppSecret());

                    context = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .scope("debug1")
                            .role("test", new String[]{"debug", "debug1"})
                            .client(ClientType.AuthorizationGrant)
                            .authenticator(AuthenticatorType.LinkedIn, config)
                            .redirect("http://valid.example.com/redirect")
                            .build();
                }
            };
    /**
     * Test container factory.
     */
    private SingletonTestContainerFactory testContainerFactory;

    /**
     * The current running test application.
     */
    private ResourceConfig testApplication;

    /**
     * Reset the facebook account before every test.
     */
    @Before
    public void linkedinLogin() {
        WebDriver d = SELENIUM.getDriver();

        By emailInput = By.id("login-email");
        By passInput = By.id("login-password");

        // Login to facebook.
        d.get("https://www.linkedin.com/");
        new WebDriverWait(d, 10)
                .until(ExpectedConditions.presenceOfElementLocated(emailInput));

        // Enter the login
        d.findElement(emailInput).clear();
        d.findElement(emailInput).sendKeys(TestConfig.getGoogleAccountId());
        d.findElement(passInput).clear();
        d.findElement(passInput).sendKeys(TestConfig.getGoogleAccountSecret());
        d.findElement(By.id("login-submit")).click();
        new WebDriverWait(d, 10000000)
                .until(ExpectedConditions.elementToBeClickable(By.id("nav-settings__dropdown-trigger")));
    }

    /**
     * Reset the linkedin account before every test.
     */
    @After
    public void linkedinLogout() {
        WebDriver d = SELENIUM.getDriver();
        d.get("https://www.linkedin.com/m/logout/");
        new WebDriverWait(d, 10)
                .until(ExpectedConditions.presenceOfElementLocated(By.id("login-email")));
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
    protected ResourceConfig createApplication() {
        if (testApplication == null) {
            testApplication = new OAuthAPI();
        }
        return testApplication;
    }

    /**
     * Configure the deployment as a servlet.
     *
     * @return The deployment context.
     */
    @Override
    protected DeploymentContext configureDeployment() {
        // This matches the port registered with facebook.
        forceSet(TestProperties.CONTAINER_PORT, TestConfig.getTestingPort());
        forceSet(TestProperties.LOG_TRAFFIC, "true");
        forceSet(TestProperties.DUMP_ENTITY, "true");

        return ServletDeploymentContext.forServlet(
                new ServletContainer(createApplication()))
                .build();
    }

    /**
     * 1. Someone new to your app logs in with Facebook
     * - Go to your app and tap on the Log in with Facebook button
     * - Tap OK to accept the read permissions
     * - Click OK again to accept write permissions if applicable
     * - Go to app settings and verify that the granted permissions are there
     */
    @Test
    public void testNewLogin() {
        String testState = RandomStringUtils.randomAlphanumeric(20);

        // Issue a request against our /authorize endpoint.
        URI requestUri = UriBuilder.fromUri(getBaseUri())
                .path("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(context.getClient().getId()))
                .queryParam("scope", "debug")
                .queryParam("state", testState)
                .build();

        WebDriver d = SELENIUM.getDriver();
        d.get(requestUri.toString());

        (new WebDriverWait(d, 10))
                .until(ExpectedConditions.urlContains("valid.example.com"));

        String url = d.getCurrentUrl();
        URI uri = URI.create(url);
        MultivaluedMap<String, String> params = HttpUtil.parseQueryParams(uri);
        BigInteger code = IdUtil.fromString(params.getFirst("code"));
        assertEquals(testState, params.getFirst("state"));

        OAuthToken authToken = getSession().get(OAuthToken.class, code);
        assertEquals(authToken.getClient().getId(),
                context.getClient().getId());
        assertEquals(AuthenticatorType.LinkedIn,
                authToken.getIdentity().getType());
        assertEquals(OAuthTokenType.Authorization,
                authToken.getTokenType());
    }
}
