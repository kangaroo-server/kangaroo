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

package net.krotscheck.kangaroo.authz.common.authenticator;

import net.krotscheck.kangaroo.authz.AuthzAPI;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.TestConfig;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import net.krotscheck.kangaroo.test.jersey.SingletonTestContainerFactory;
import net.krotscheck.kangaroo.test.rule.SeleniumRule;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import net.krotscheck.kangaroo.test.runner.SingleInstanceTestRunner;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType.Facebook;
import static net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType.Github;
import static net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType.Google;
import static net.krotscheck.kangaroo.authz.common.authenticator.oauth2.AbstractOAuth2Authenticator.CLIENT_ID_KEY;
import static net.krotscheck.kangaroo.authz.common.authenticator.oauth2.AbstractOAuth2Authenticator.CLIENT_SECRET_KEY;

/**
 * An abstract testing class that sets up the browser harness and a running
 * application instance. Also includes all runtime data, for an application
 * that contains one-of-each login method.
 *
 * @author Michael Krotscheck
 */
@RunWith(SingleInstanceTestRunner.class)
public abstract class AbstractBrowserLoginTest extends ContainerTest {

    /**
     * The selenium rule.
     */
    @ClassRule
    public static final SeleniumRule SELENIUM = new SeleniumRule();

    /**
     * Timeout, in seconds, used to wait for specific driver changes.
     */
    protected static final Integer TIMEOUT = 20;

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

                    Map<String, String> googleConfig = new HashMap<>();
                    googleConfig.put(CLIENT_ID_KEY,
                            TestConfig.getGoogleAppId());
                    googleConfig.put(CLIENT_SECRET_KEY,
                            TestConfig.getGoogleAppSecret());

                    Map<String, String> fbConfig = new HashMap<>();
                    fbConfig.put(CLIENT_ID_KEY,
                            TestConfig.getFacebookAppId());
                    fbConfig.put(CLIENT_SECRET_KEY,
                            TestConfig.getFacebookAppSecret());

                    Map<String, String> githubConfig = new HashMap<>();
                    githubConfig.put(CLIENT_ID_KEY,
                            TestConfig.getGithubAppId());
                    githubConfig.put(CLIENT_SECRET_KEY,
                            TestConfig.getGithubAppSecret());

                    context = ApplicationBuilder
                            .newApplication(session)
                            .scope("debug")
                            .scope("debug1")
                            .role("test", new String[]{"debug", "debug1"})
                            .client(ClientType.AuthorizationGrant)
                            .authenticator(Google, googleConfig)
                            .authenticator(Facebook, fbConfig)
                            .authenticator(Github, githubConfig)
                            .redirect("https://www.example.com/")
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
     * Get the application data context under test.
     *
     * @return The data snapshot for the current testing application.
     */
    public static ApplicationContext getContext() {
        return context;
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
    protected final TestContainerFactory getTestContainerFactory()
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
            testApplication = new AuthzAPI();
        }
        return testApplication;
    }

    /**
     * Configure the deployment as a servlet.
     *
     * @return The deployment context.
     */
    @Override
    protected final DeploymentContext configureDeployment() {
        forceSet(TestProperties.CONTAINER_PORT, TestConfig.getTestingPort());
        forceSet(TestProperties.LOG_TRAFFIC, "true");
        forceSet(TestProperties.DUMP_ENTITY, "true");

        return ServletDeploymentContext.forServlet(
                new ServletContainer(createApplication()))
                .build();
    }
}
