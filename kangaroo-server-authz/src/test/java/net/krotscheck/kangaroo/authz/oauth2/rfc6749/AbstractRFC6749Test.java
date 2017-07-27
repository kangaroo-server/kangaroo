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

import net.krotscheck.kangaroo.authz.admin.AdminV1API;
import net.krotscheck.kangaroo.authz.common.authenticator.test.TestAuthenticator;
import net.krotscheck.kangaroo.authz.oauth2.OAuthAPI;
import net.krotscheck.kangaroo.test.jerseyTest.ContainerTest;
import net.krotscheck.kangaroo.test.jerseyTest.SingletonTestContainerFactory;
import net.krotscheck.kangaroo.test.runner.SingleInstanceTestRunner;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.runner.RunWith;

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
     * The current running test application.
     */
    private ResourceConfig testApplication;

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
