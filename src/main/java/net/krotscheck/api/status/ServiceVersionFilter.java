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

package net.krotscheck.api.status;

import net.krotscheck.features.config.SystemConfiguration;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * This service response decorator attaches the API version to the response.
 *
 * @author Michael Krotscheck
 */
public final class ServiceVersionFilter implements ContainerResponseFilter {

    /**
     * Version to attach to the response context.
     */
    private String version;

    /**
     * Create a new instance of this response filter.
     *
     * @param config The system configuration, injected.
     */
    @Inject
    public ServiceVersionFilter(final SystemConfiguration config) {
        this.version = config.getVersion();
    }

    /**
     * Apply the filter, by attaching the API version to every response.
     *
     * @param requestContext  The jersey request context.
     * @param responseContext The jersey response context.
     * @throws IOException Not thrown.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext)
            throws IOException {

        responseContext.getHeaders().add("API-Version", version);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(ServiceVersionFilter.class)
                    .to(ContainerResponseFilter.class)
                    .in(Singleton.class);
        }
    }
}
