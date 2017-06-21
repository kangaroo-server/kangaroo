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

package net.krotscheck.kangaroo.util;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This utility assists in extracting simple values from a Jersey request
 * context.
 *
 * @author Michael Krotscheck
 */
public final class RequestUtil {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory.getLogger(RequestUtil.class);

    /**
     * Utility class, private constructor.
     */
    private RequestUtil() {

    }

    /**
     * Provided with a request context, will extract the CORS requested HTTP
     * Method.
     *
     * @param request The request to analyze.
     * @return The HTTP Request Method.
     */
    public static String getCORSRequestedMethod(
            final ContainerRequestContext request) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        return headers
                .getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    }

    /**
     * Provided with a request context, will extract the CORS requested HTTP
     * Headers.
     *
     * @param request The request to analyze.
     * @return The HTTP Request Method.
     */
    public static List<String> getCORSRequestedHeaders(
            final ContainerRequestContext request) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        String rawHeaders = headers
                .getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (Strings.isNullOrEmpty(rawHeaders)) {
            return Collections.emptyList();
        }

        return Arrays.stream(rawHeaders.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !Strings.isNullOrEmpty(s))
                .collect(Collectors.toList());
    }

    /**
     * Provided with a request context, this method will extract the CORS
     * origin header.
     *
     * @param request The request to analyze.
     * @return Origin header as a URI.
     */
    public static URI getOrigin(final ContainerRequestContext request) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        String rawHost = headers.getFirst(HttpHeaders.ORIGIN);

        try {
            return new URIBuilder(rawHost).build();
        } catch (URISyntaxException | NullPointerException use) {
            return null;
        }
    }

    /**
     * Provided with a request context, this method will extract the
     * Referrer, or return null.
     *
     * @param request The request to analyze.
     * @return Origin header as a URI.
     */
    public static URI getReferer(final ContainerRequestContext request) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        String rawHost = headers.getFirst(HttpHeaders.REFERER);
        try {
            return new URIBuilder(rawHost).build();
        } catch (URISyntaxException | NullPointerException use) {
            return null;
        }
    }

    /**
     * Provided with a request context, this method will extract the host,
     * port, and protocol into a simple URI. If the request does not contain
     * any of these, it will throw an exception.
     *
     * @param request The request to analyze.
     * @return The host as a URI, or null.
     */
    public static URI getHost(final ContainerRequestContext request) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        String rawHost = headers.getFirst(HttpHeaders.HOST);

        try {
            // Split into host and port if necessary
            String[] parts = rawHost.split(":");

            URIBuilder builder = new URIBuilder();
            builder.setScheme(request.getUriInfo().getRequestUri().getScheme());

            builder.setHost(parts[0]);

            if (parts.length > 1) {
                builder.setPort(Integer.valueOf(parts[1]));
            }

            return builder.build();
        } catch (URISyntaxException | NumberFormatException
                | NullPointerException e) {
            throw new BadRequestException(e);
        }
    }

    /**
     * Provided with a request, attempts to extract various X-Forwarded-*
     * headers into a full URI. This method will return null unless a
     * protocol and host are set.
     *
     * @param request The request from which to extract the X-Forwarded-Host.
     * @return The Forwarded Host URI, or null.
     */
    public static URI getForwardedHost(
            final ContainerRequestContext request) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        String rawForwardedHost =
                headers.getFirst(HttpHeaders.X_FORWARDED_HOST);
        String rawForwardedPort =
                headers.getFirst(HttpHeaders.X_FORWARDED_PORT);
        String rawForwardedProto =
                headers.getFirst(HttpHeaders.X_FORWARDED_PROTO);

        try {
            Integer forwardedPort = rawForwardedPort == null ? -1
                    : Integer.valueOf(rawForwardedPort, 10);

            URI result = new URIBuilder()
                    .setHost(rawForwardedHost)
                    .setPort(forwardedPort)
                    .setScheme(rawForwardedProto)
                    .build();
            return Strings.isNullOrEmpty(result.toString()) ? null : result;
        } catch (URISyntaxException | NumberFormatException use) {
            logger.debug("Cannot parse forwarded header.", use);
            return null;
        }
    }

    /**
     * This method attempts to determine if the request is a cross-origin
     * request. This is not a check for CORS, as that standard demands the
     * Origin header be set. Instead, it implements the OWASP Cross Origin
     * detection algorithm.
     *
     * @param request The request from which to extract the X-Forwarded-Host.
     * @return True if this is an OWASP-evaluated cross-origin request.
     * @throws BadRequestException Thrown if the source or target cannot be
     *                             determined from the provided headers.
     * @see <a href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet#Verifying_Same_Origin_with_Standard_Headers">https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet#Verifying_Same_Origin_with_Standard_Headers</a>
     */
    public static Boolean isCrossOriginRequest(
            final ContainerRequestContext request)
            throws BadRequestException {

        URI origin = getOrigin(request);
        URI referer = getReferer(request);
        URI host = getHost(request); // This will throw an exception.
        URI forwardedHost = getForwardedHost(request);

        try {
            referer = new URIBuilder()
                    .setScheme(referer.getScheme())
                    .setHost(referer.getHost())
                    .setPort(referer.getPort())
                    .build();
        } catch (NullPointerException | URISyntaxException e) {
            referer = null;
        }

        // Try to find at least one target and source.
        URI source = origin == null ? referer : origin;
        URI target = forwardedHost == null ? host : forwardedHost;

        // This request is bad, exit and return.
        if (source == null) {
            return false;
        }

        return !source.equals(target);
    }
}
