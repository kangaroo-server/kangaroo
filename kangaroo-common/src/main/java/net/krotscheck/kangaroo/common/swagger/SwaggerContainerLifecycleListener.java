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

import io.swagger.jaxrs.config.BeanConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * This container lifecycle listener initializes the Swagger resource
 * scanner, with parameters that are only available after the context has
 * been initialized.
 *
 * @author Michael Krotscheck
 */
public final class SwaggerContainerLifecycleListener
        implements ContainerLifecycleListener {

    /**
     * The class path, used to constrain the resources available via this UI.
     */
    private final String classPath;

    /**
     * The servlet context, used to extract scanner ID and base path.
     */
    @Inject
    private ServletContext context;

    /**
     * Constructor, which does NOT use injection. Provide the required
     * classPath, everything else is automatic.
     *
     * @param classPath The classpath to scan.
     */
    public SwaggerContainerLifecycleListener(final String classPath) {
        this.classPath = classPath;
    }

    /**
     * Getter for the servlet context.
     *
     * @return The servlet context.
     */
    public ServletContext getContext() {
        return context;
    }

    /**
     * Setter for the servlet context.
     *
     * @param context The new servlet context.
     */
    public void setContext(final ServletContext context) {
        this.context = context;
    }

    /**
     * When the container starts up, extract the required context parameters
     * and run the scanner.
     *
     * @param container The container.
     */
    @Override
    public void onStartup(final Container container) {
        String name = context.getServletContextName();
        ServletRegistration registration = context.getServletRegistration(name);
        String scannerId = registration.getInitParameter("swagger.context.id");
        String basePath = context.getContextPath();

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(classPath);
        beanConfig.setScannerId(scannerId);
        beanConfig.setBasePath(basePath);
        beanConfig.setUsePathBasedConfig(true);
        beanConfig.setScan(true);
    }

    /**
     * Do nothing on reload.
     *
     * @param container The container.
     */
    @Override
    public void onReload(final Container container) {

    }

    /**
     * Do nothing on shutdown.
     *
     * @param container The container.
     */
    @Override
    public void onShutdown(final Container container) {

    }
}
