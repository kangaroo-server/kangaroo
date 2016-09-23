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
 *
 */

package net.krotscheck.kangaroo.common.logging;

import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Unit tests for the java.util.logging -> slf4j redirect.
 *
 * @author Michael Krotscheck
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SLF4JBridgeHandler.class)
public final class JULRedirectContextListenerTest {

    /**
     * Test that the logger is installed on startup.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void onStartup() throws Exception {
        Container c = Mockito.mock(Container.class);

        ContainerLifecycleListener listener = new JULRedirectContextListener();

        Assert.assertFalse(SLF4JBridgeHandler.isInstalled());
        listener.onStartup(c);
        listener.onStartup(c);
        Assert.assertTrue(SLF4JBridgeHandler.isInstalled());
    }

    /**
     * Does nothing. Coverage only.
     */
    @Test
    public void onReload() {
        Container c = Mockito.mock(Container.class);
        ContainerLifecycleListener listener =
                new JULRedirectContextListener();
        listener.onReload(c);
        Mockito.verifyNoMoreInteractions(c);
    }

    /**
     * Does nothing. Coverage only.
     */
    @Test
    public void onShutdown() {
        Container c = Mockito.mock(Container.class);
        ContainerLifecycleListener listener =
                new JULRedirectContextListener();
        listener.onShutdown(c);
        Mockito.verifyNoMoreInteractions(c);
    }
}
