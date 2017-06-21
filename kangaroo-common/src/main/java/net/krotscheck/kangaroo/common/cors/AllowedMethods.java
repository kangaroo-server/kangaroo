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

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 *
 */
public final class AllowedMethods extends AbstractBinder {

    /**
     * The name used for this injection point.
     */
    public static final String NAME = "CORS_ALLOWED_METHODS";

    /**
     * List of permitted CORS headers to inject.
     */
    private final String[] permittedHeaders;

    /**
     * @param headers The list of headers to provide to CORS as valid request
     *                headers.
     */
    public AllowedMethods(final String[] headers) {
        this.permittedHeaders = headers;
    }

    /**
     * Implement to provide binding definitions using the exposed binding
     * methods.
     */
    @Override
    protected void configure() {
        for (String header : permittedHeaders) {
            bind(header)
                    .to(String.class)
                    .named(NAME);
        }
    }
}
