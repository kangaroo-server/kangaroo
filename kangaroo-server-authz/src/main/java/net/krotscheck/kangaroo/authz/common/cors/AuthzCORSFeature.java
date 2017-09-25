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

package net.krotscheck.kangaroo.authz.common.cors;

import net.krotscheck.kangaroo.common.cors.CORSFeature;
import net.krotscheck.kangaroo.common.cors.ExposedHeaders;
import net.krotscheck.kangaroo.common.response.ApiParam;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * CORS Feature, specifically configured for the Admin API.
 *
 * @author Michael Krotscheck
 */
public final class AuthzCORSFeature implements Feature {

    /**
     * Configure the CORS feature for the Authz service.
     *
     * @param context configurable context in which the
     *                feature should be enabled.
     * @return {@code true} if the feature was successfully enabled.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Inject the CORS feature.
        context.register(CORSFeature.class);

        // Add custom headers & Methods
        context.register(new ExposedHeaders(new String[]{
                ApiParam.LIMIT_HEADER,
                ApiParam.OFFSET_HEADER,
                ApiParam.ORDER_HEADER,
                ApiParam.SORT_HEADER,
                ApiParam.TOTAL_HEADER}));

        // Add the cors validator.
        context.register(new HibernateCORSCacheLoader.Binder());
        context.register(new HibernateCORSValidator.Binder());

        return true;
    }
}
