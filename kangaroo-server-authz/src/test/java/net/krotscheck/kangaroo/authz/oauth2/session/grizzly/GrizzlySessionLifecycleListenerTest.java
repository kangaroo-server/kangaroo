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

package net.krotscheck.kangaroo.authz.oauth2.session.grizzly;

import net.krotscheck.kangaroo.authz.AuthzServerConfig;
import net.krotscheck.kangaroo.common.config.SystemConfiguration;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.server.spi.Container;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.SessionCookieConfig;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for the lifecycle listener test. Its job is to replace the
 * grizzly session manager with one that's attached to the injection context.
 *
 * @author Michael Krotscheck
 */
public final class GrizzlySessionLifecycleListenerTest extends DatabaseTest {

    /**
     * The listener under test.
     */
    private GrizzlySessionLifecycleListener listener;

    /**
     * The container to inject.
     */
    private Container mockContainer;

    /**
     * The mock servlet context..
     */
    private WebappContext context;

    /**
     * The mock session manager.
     */
    private GrizzlySessionManager sessionManager;

    /**
     * The mock configuration.
     */
    private SystemConfiguration configuration;

    /**
     * Bootstrap the test.
     */
    @Before
    public void setupTest() {
        mockContainer = mock(Container.class);
        context = mock(WebappContext.class);
        SessionCookieConfig config =
                new org.glassfish.grizzly.servlet.SessionCookieConfig(context);
        doReturn(config).when(context).getSessionCookieConfig();

        configuration = new SystemConfiguration(Collections.emptyList());
        sessionManager = new GrizzlySessionManager(configuration,
                this::getSessionFactory);

        listener = new GrizzlySessionLifecycleListener(
                context, sessionManager, configuration);
    }

    /**
     * Assert that the session manager is replaced on startup.
     */
    @Test
    public void onStartup() {
        listener.onStartup(mockContainer);
        verifyNoMoreInteractions(mockContainer);
        verify(context, times(1))
                .setSessionManager(sessionManager);
    }

    /**
     * Assert that the cookie configuration is overridden on startup.
     */
    @Test
    public void onStartupConfigureCookie() {
        listener.onStartup(mockContainer);
        verifyNoMoreInteractions(mockContainer);

        SessionCookieConfig config = context.getSessionCookieConfig();

        assertTrue(config.isHttpOnly());
        assertTrue(config.isSecure());
        assertEquals(config.getMaxAge(),
                (long) AuthzServerConfig.SESSION_MAX_AGE.getValue());
        assertEquals(config.getName(),
                AuthzServerConfig.SESSION_NAME.getValue());
    }

    /**
     * Assert that nothing happens on reload.
     */
    @Test
    public void onReload() {
        listener.onReload(mockContainer);
        verifyNoMoreInteractions(mockContainer);
    }

    /**
     * Assert that nothing happens on shutdown.
     */
    @Test
    public void onShutdown() {
        listener.onShutdown(mockContainer);
        verifyNoMoreInteractions(mockContainer);
    }
}
