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

package net.krotscheck.kangaroo.common.security;

import net.krotscheck.kangaroo.server.SecurityHeaders;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

/**
 * HTTP headers which need to decorate most request responses.
 *
 * @author Michael Krotscheck
 */
@PreMatching
public final class SecurityHeadersFilter implements ContainerResponseFilter {

    /**
     * Decorate the response with any security-related headers that we need.
     *
     * @param requestContext  request context.
     * @param responseContext response context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) throws
            IOException {

        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        SecurityHeaders.ALL.forEach(headers::add);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(SecurityHeadersFilter.class)
                    .to(ContainerResponseFilter.class)
                    .in(Singleton.class);
        }
    }
}
