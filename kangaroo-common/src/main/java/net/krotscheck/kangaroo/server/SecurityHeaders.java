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

package net.krotscheck.kangaroo.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;

import java.util.Map;

/**
 * A small class that stores all of our requred HTTP Seucrity headers. We
 * don't really discriminate between API and static asset responses here.
 *
 * @author Michael Krotscheck
 */
public final class SecurityHeaders {

    /**
     * List of all HTTP headers that must be attached to all responses.
     */
    public static final Map<String, String> ALL =
            ImmutableMap.<String, String>builder()
                    .put(HttpHeaders.X_FRAME_OPTIONS, "Deny")
                    .build();

    /**
     * Utility class, private constructor.
     */
    private SecurityHeaders() {
    }
}
