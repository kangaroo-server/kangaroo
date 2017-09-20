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

package net.krotscheck.kangaroo.common.cors;

import com.google.common.base.Strings;
import com.google.common.collect.Streams;
import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.util.RequestUtil;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This request filter handles all CORS requests for the system.
 *
 * @author Michael Krotscheck
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public final class CORSFilter implements ContainerResponseFilter {

    /**
     * List of allowed headers.
     */
    private final List<String> allowedHeaders;

    /**
     * List of allowed methods.
     */
    private final List<String> allowedMethods;

    /**
     * List of exposed headers.
     */
    private final List<String> exposedHeaders;

    /**
     * List of allowed methods.
     */
    private final ICORSValidator validator;

    /**
     * Create a new CORS filter.
     *
     * @param allowedHeaders Injected allowed headers.
     * @param allowedMethods Injected allowed methods.
     * @param exposedHeaders Injected exposed methods.
     * @param validator      Validation handler. Should be provided by the
     *                       consuming service.
     */
    @Inject
    public CORSFilter(@Named(AllowedHeaders.NAME) final
                      Iterable<String> allowedHeaders,
                      @Named(AllowedMethods.NAME) final
                      Iterable<String> allowedMethods,
                      @Named(ExposedHeaders.NAME) final
                      Iterable<String> exposedHeaders,
                      final ICORSValidator validator) {
        this.allowedHeaders = Streams.stream(allowedHeaders)
                .filter(h -> !Strings.isNullOrEmpty(h))
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        this.allowedMethods = Streams.stream(allowedMethods)
                .filter(h -> !Strings.isNullOrEmpty(h))
                .collect(Collectors.toList());
        this.exposedHeaders = Streams.stream(exposedHeaders)
                .filter(h -> !Strings.isNullOrEmpty(h))
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        this.validator = validator;
    }

    /**
     * This filter handles CORS responses. By this time, if it's a CORS
     * request, we already know that the origin is valid, so we can proceed
     * based on that.
     *
     * @param request  Request context.
     * @param response Response context.
     * @throws IOException if an I/O exception occurs.
     * @see PreMatching
     */
    @Override
    public void filter(final ContainerRequestContext request,
                       final ContainerResponseContext response)
            throws IOException {
        // All CORS responses vary on Origin.
        response.getHeaders().addAll(HttpHeaders.VARY, HttpHeaders.ORIGIN);

        // If the origin header is not set, or not valid, exit.
        URI origin = RequestUtil.getOrigin(request);
        if (origin == null || !validator.isValidCORSOrigin(origin)) {
            return;
        }

        // Handle a preflight request.
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            preflightFilter(origin, request, response);
        } else if (allowedMethods.contains(request.getMethod())) {
            requestFilter(origin, response);
        }
    }

    /**
     * Annotate an OPTIONS response with the appropriate CORS headers.
     *
     * @param origin   The validated origin
     * @param request  The request, with headers.
     * @param response The response to annotate.
     */
    private void preflightFilter(final URI origin,
                                 final ContainerRequestContext request,
                                 final ContainerResponseContext response) {

        MultivaluedMap<String, Object> resHeaders = response.getHeaders();

        // If the underlying resource doesn't exist, take over the status code.
        if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            response.setStatus(Status.OK.getStatusCode());
        }

        // Annotate the VARY headers.
        resHeaders.add(HttpHeaders.VARY,
                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        resHeaders.add(HttpHeaders.VARY,
                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);

        // Exit if the method is not permitted.
        String method = RequestUtil.getCORSRequestedMethod(request);
        if (!allowedMethods.contains(method)) {
            return;
        }
        resHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, method);

        // Echo the origin (since it's been validated.
        resHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);

        // Credentials and lifetime.
        resHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        resHeaders.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, 5 * 60);

        // Build the intersection of requested and permitted headers
        List<String> requestedHeaders =
                RequestUtil.getCORSRequestedHeaders(request);
        allowedHeaders.stream()
                .filter(requestedHeaders::contains)
                .forEach(h -> resHeaders
                        .add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, h));
    }

    /**
     * Annotate the response for a regular response with the appropriate CORS
     * headers. At this point, the request has already been validated.
     *
     * @param origin   The validated origin.
     * @param response The response to annotate.
     */
    private void requestFilter(final URI origin,
                               final ContainerResponseContext response) {

        // Grab the headers that have already been sent.s
        MultivaluedMap<String, Object> headers = response.getHeaders();

        // Origin
        headers.add(HttpHeaders.ORIGIN, origin);

        // Interpolate the exposed headers.
        List<Object> declaredExposedHeaders =
                headers.keySet().stream()
                        .map(String::toLowerCase)
                        .filter(this.exposedHeaders::contains)
                        .collect(Collectors.toList());
        headers.addAll(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                declaredExposedHeaders);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(CORSFilter.class)
                    .to(ContainerResponseFilter.class)
                    .in(Singleton.class);
        }
    }
}
