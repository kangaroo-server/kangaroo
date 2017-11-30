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

package net.krotscheck.kangaroo.common.swagger;

import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Swagger API method scanning and documentation feature.
 *
 * @author Michael Krotscheck
 */
public final class SwaggerFeature implements Feature {

    /**
     * The classpath to scan for swagger resources.
     */
    private final String classPath;

    /**
     * Create a new instance of the swagger feature, scanning a particular
     * classpath.
     *
     * @param classPath The classpath to scan.
     */
    public SwaggerFeature(final String classPath) {
        this.classPath = classPath;
    }

    /**
     * Initialize the swagger feature with the provided classpath.
     *
     * @param context The application context.
     * @return Always true.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Ensure the Beanconfig is set up.
        context.register(new SwaggerContainerLifecycleListener(classPath));

        // A service to host the UI.
        context.register(SwaggerUIService.class);

        // Register the swagger.json and serializer resources (Third party).
        context.register(ApiListingResource.class);
        context.register(SwaggerSerializers.class);
        return true;
    }
}
