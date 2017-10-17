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

package net.krotscheck.kangaroo.common.httpClient;

import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.common.security.SecurityFeature;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Singleton;
import java.util.function.Supplier;

/**
 * This factory provides a singleton JerseyClient builder that is
 * preconfigured with all necessary entities, parsers, and keystores.
 *
 * @author Michael Krotscheck
 */
public final class JerseyClientBuilderFactory
        implements Supplier<JerseyClientBuilder> {

    /**
     * Create the application's Jersey Client builder.
     *
     * @return Our configured client builder.
     */
    @Override
    public JerseyClientBuilder get() {
        JerseyClientBuilder newBuilder = new JerseyClientBuilder();
        newBuilder.register(JacksonFeature.class);
        newBuilder.register(SecurityFeature.class);

        return newBuilder;
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(JerseyClientBuilderFactory.class)
                    .to(JerseyClientBuilder.class)
                    .in(Singleton.class);
        }
    }
}
