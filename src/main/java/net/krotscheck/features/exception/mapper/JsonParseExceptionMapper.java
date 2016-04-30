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

package net.krotscheck.features.exception.mapper;


import com.fasterxml.jackson.core.JsonParseException;
import net.krotscheck.features.exception.ErrorResponseBuilder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Implementation of {@link ExceptionMapper} to send down a "400 Bad Request" in
 * the event unparsable JSON is received.
 */
@Provider
public final class JsonParseExceptionMapper
        implements ExceptionMapper<JsonParseException> {

    /**
     * Return the exception as a JSON response.
     *
     * @param e The parse exceptions to handle.
     */
    @Override
    public Response toResponse(final JsonParseException e) {
        return ErrorResponseBuilder
                .from(e)
                .build();
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(JsonParseExceptionMapper.class)
                    .to(ExceptionMapper.class)
                    .in(Singleton.class);
        }
    }
}

