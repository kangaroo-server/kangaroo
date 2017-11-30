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

package net.krotscheck.kangaroo.common.swagger;

import io.swagger.config.Scanner;
import io.swagger.jaxrs.config.SwaggerScannerLocator;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the initial container lifecycle listener.
 *
 * @author Michael Krotscheck
 */
public final class SwaggerContainerLifecycleListenerTest {

    /**
     * The injector (This is a mockito mock).
     */
    private InjectionManager injectionManager;

    /**
     * The class under test.
     */
    private SwaggerContainerLifecycleListener listener;

    /**
     * Test setup.
     */
    @Before
    public void setupLifecycleListener() {
        ServletContext context = mock(ServletContext.class);
        ServletRegistration mockRegistration = mock(ServletRegistration.class);
        doReturn("Name")
                .when(context)
                .getServletContextName();
        doReturn(mockRegistration)
                .when(context)
                .getServletRegistration("Name");
        doReturn("ScannerId")
                .when(mockRegistration).getInitParameter("swagger.context.id");
        doReturn("/v1")
                .when(context)
                .getContextPath();

        injectionManager = mock(InjectionManager.class);
        listener = new SwaggerContainerLifecycleListener(
                "net.krotscheck.kangaroo");
        listener.setContext(context);
    }

    /**
     * Assert that getting/setting the context works.
     */
    @Test
    public void getSetContext() {
        ServletContext mockContext = mock(ServletContext.class);

        assertNotSame(mockContext, listener.getContext());
        listener.setContext(mockContext);
        assertSame(mockContext, listener.getContext());
    }

    /**
     * Assert that a scanner is created on startup.
     */
    @Test
    public void onStartup() {
        Container container = mock(Container.class);
        listener.onStartup(container);
        Mockito.verifyNoMoreInteractions(container);

        SwaggerScannerLocator locator = SwaggerScannerLocator.getInstance();
        Scanner scanner = locator.getScanner("ScannerId");
        assertNotNull(scanner);
    }

    /**
     * Assert that reload does nothing.
     */
    @Test
    public void onReload() {
        Container container = mock(Container.class);
        listener.onReload(container);
        Mockito.verifyNoMoreInteractions(container);
    }

    /**
     * Assert that shutdown does nothing.
     */
    @Test
    public void onShutdown() {
        Container container = mock(Container.class);
        listener.onShutdown(container);
        Mockito.verifyNoMoreInteractions(container);
    }
}