/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.common.logging;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature's purpose is to intercept events within the jersey system, and
 * log them to support generic operational.
 *
 * @author Michael Krotscheck
 */
public final class LoggingFeature implements Feature {

    /**
     * Register this feature.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Jersey2 Logging feature, for trace-level debugging.
        context.register(org.glassfish.jersey.logging.LoggingFeature.class);

        // Our own INFO-level logger.
        context.register(HttpResponseLoggingFilter.class);

        return true;
    }
}
