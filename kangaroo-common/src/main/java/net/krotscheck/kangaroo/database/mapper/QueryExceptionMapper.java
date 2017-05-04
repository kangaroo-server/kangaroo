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

package net.krotscheck.kangaroo.database.mapper;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hibernate.QueryException;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An exception mapper that handles query exceptions. These usually indicate
 * that hibernate was unable to translate a query to the underlying SQL
 * syntax, and thus indicates bad input parameters such as sort fields and
 * such.
 *
 * @author Michael Krotscheck
 */
public final class QueryExceptionMapper
        implements ExceptionMapper<QueryException> {

    /**
     * Convert to response.
     *
     * @param exception The exception to convert.
     * @return A Response instance for this error.
     */
    public Response toResponse(final QueryException exception) {
        return ErrorResponseBuilder
                .from(Status.BAD_REQUEST)
                .build();
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(QueryExceptionMapper.class)
                    .to(ExceptionMapper.class)
                    .in(Singleton.class);
        }
    }
}
