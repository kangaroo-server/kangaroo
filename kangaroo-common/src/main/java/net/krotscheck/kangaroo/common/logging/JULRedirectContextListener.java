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

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.annotation.Priority;
import javax.inject.Singleton;

/**
 * This container lifecycle listener ensures that all java.util.logging
 * messages are piped into SLF4J.
 *
 * @author Michael Krotscheck
 */
@Priority(0)
public final class JULRedirectContextListener
        implements ContainerLifecycleListener {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(JULRedirectContextListener.class);

    /**
     * Install the handler, and uninstall any existing root handlers.
     *
     * @param container The container for this servlet.
     */
    @Override
    public void onStartup(final Container container) {
        if (!SLF4JBridgeHandler.isInstalled()) {
            logger.info("Installing SLF4JBridgeHandler");
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }

    /**
     * Do nothing.
     *
     * @param container The container for this servlet.
     */
    @Override
    public void onReload(final Container container) {

    }

    /**
     * Do nothing.
     *
     * @param container The container for this servlet.
     */
    @Override
    public void onShutdown(final Container container) {

    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(JULRedirectContextListener.class)
                    .to(ContainerLifecycleListener.class)
                    .in(Singleton.class);
        }
    }
}
