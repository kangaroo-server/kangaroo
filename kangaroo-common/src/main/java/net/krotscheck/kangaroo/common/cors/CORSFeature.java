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

import com.google.common.net.HttpHeaders;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * CORS Feature, including extendable configuration injectors, and a request
 * filter that ensures content is only shared as desired.
 *
 * @author Michael Krotscheck
 */
public final class CORSFeature implements Feature {

    /**
     * Register this feature.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Inject a set of sane defaults that a client may request. If you'd
        // like to add more headers, please add a similar injector to the
        // appropriate module.
        context.register(new AllowedHeaders(new String[]{
                HttpHeaders.ACCEPT,
                HttpHeaders.ACCEPT_LANGUAGE,
                HttpHeaders.CONTENT_LANGUAGE,
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ORIGIN,
                HttpHeaders.X_REQUESTED_WITH
        }));
        // Inject a set of basic HTTP methods.
        context.register(new AllowedMethods(new String[]{
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.POST,
                HttpMethod.DELETE,
                HttpMethod.OPTIONS
        }));
        // Inject a set of basic headers that can be exposed.
        context.register(new ExposedHeaders(new String[]{
                HttpHeaders.LOCATION,
                HttpHeaders.WWW_AUTHENTICATE,
                HttpHeaders.CACHE_CONTROL,
                HttpHeaders.CONTENT_LANGUAGE,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.EXPIRES,
                HttpHeaders.LAST_MODIFIED,
                HttpHeaders.PRAGMA,
        }));

        context.register(new CORSFilter.Binder());
        return true;
    }
}
