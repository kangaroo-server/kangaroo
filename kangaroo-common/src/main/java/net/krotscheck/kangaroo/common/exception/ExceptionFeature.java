/*
 * Copyright (c) 2016 Michael Krotscheck
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
 */

package net.krotscheck.kangaroo.common.exception;

import net.krotscheck.kangaroo.common.exception.mapper.JerseyExceptionMapper;
import net.krotscheck.kangaroo.common.exception.mapper.JsonParseExceptionMapper;
import net.krotscheck.kangaroo.common.exception.mapper.UnhandledExceptionMapper;
import net.krotscheck.kangaroo.common.exception.mapper.HttpStatusExceptionMapper.Binder;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature includes our mapping logic for requests and responses. This
 * includes exception mapping, multipart responses, multipart batch request
 * handling, and others. Feature specific filters (such as OpenID, etc) should
 * be included in the appropriate feature rather than here.
 *
 * @author Michael Krotscheck
 */
public final class ExceptionFeature implements Feature {

    /**
     * Register this feature.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Exception mappers.
        context.register(new Binder());
        context.register(new JerseyExceptionMapper.Binder());
        context.register(new JsonParseExceptionMapper.Binder());
        context.register(new UnhandledExceptionMapper.Binder());

        return true;
    }
}
