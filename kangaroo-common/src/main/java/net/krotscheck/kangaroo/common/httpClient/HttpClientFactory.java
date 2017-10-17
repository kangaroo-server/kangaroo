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

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.process.internal.RequestScoped;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

/**
 * This Jersey2 feature permits the creation and injection of a common HTTP
 * Client instance that may be used to make outgoing api requests. These
 * clients are generated on a per-request basis - only the factory is
 * considered a singleton - in order to ensure that every HTTP request is
 * closed.
 *
 * @author Michael Krotscheck
 */
public final class HttpClientFactory implements DisposableSupplier<Client> {

    /**
     * The Jersey client builder.
     */
    private final JerseyClientBuilder builder;

    /**
     * Create a jersey client builder.
     *
     * @param builder The client builder.
     */
    @Inject
    public HttpClientFactory(final JerseyClientBuilder builder) {
        this.builder = builder;
    }

    /**
     * Create a new client instance.
     *
     * @return The created instance.
     */
    @Override
    public Client get() {
        return builder.build();
    }

    /**
     * Ensure that all outstanding requests created by the client are closed.
     *
     * @param instance The client instance to dispose of.
     */
    @Override
    public void dispose(final Client instance) {
        instance.close();
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(HttpClientFactory.class)
                    .to(Client.class)
                    .in(RequestScoped.class);
        }
    }
}
