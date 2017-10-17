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

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This Jersey2 feature permits the creation and injection of a common HTTP
 * Client instance that may be used to make outgoing api requests.
 *
 * @author Michael Krotscheck
 */
public final class HttpClientFeature implements Feature {

    /**
     * Register all necessary components for the HTTP client feature.
     *
     * @param context The application feature context.
     * @return True.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // The client builder...
        context.register(new JerseyClientBuilderFactory.Binder());

        // And the per-request generated client.
        context.register(new HttpClientFactory.Binder());

        return true;
    }
}
