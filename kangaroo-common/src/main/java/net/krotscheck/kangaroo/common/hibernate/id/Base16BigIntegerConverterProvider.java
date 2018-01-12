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

package net.krotscheck.kangaroo.common.hibernate.id;

import org.glassfish.jersey.internal.inject.Custom;

import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.FormParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Jersey2 Context parameter converter for base16 to bigInteger conversion.
 * Restricted to PathParams and QueryParams.
 *
 * @author Michael Krotscheck
 */
@Provider
@Singleton
@Custom
public final class Base16BigIntegerConverterProvider
        implements ParamConverterProvider {

    /**
     * Return a converter if we know how to handle the type.
     *
     * @param clazz       The class to check.
     * @param type        The java type.
     * @param annotations Any additional annotations.
     * @param <T>         The type to handle.
     * @return A parameter converter, if we know how to handle this.
     */
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> clazz,
                                              final Type type,
                                              final Annotation[] annotations) {
        if (!clazz.getName().equals(BigInteger.class.getName())) {
            return null;
        }

        for (Annotation a : Arrays.asList(annotations)) {
            if (a instanceof PathParam) {
                return new BigIntConverter<>(NotFoundException::new);
            }
            if (a instanceof QueryParam) {
                return new BigIntConverter<>(BadRequestException::new);
            }
            if (a instanceof FormParam) {
                return new BigIntConverter<>(BadRequestException::new);
            }
        }

        return null;
    }

    /**
     * The parameter converter class.
     *
     * @param <T> Raw type, required for converters.
     */
    private static final class BigIntConverter<T>
            implements ParamConverter<T> {

        /**
         * The exception provider.
         */
        private final Supplier<? extends WebApplicationException> supplier;

        /**
         * Create a new instance of the converter.
         *
         * @param supplier An exception supplier.
         */
        BigIntConverter(
                final Supplier<? extends WebApplicationException> supplier) {
            this.supplier = supplier;
        }

        /**
         * Convert from an int to a value.
         *
         * @param value The string value.
         * @return The BigInteger result.
         */
        @SuppressWarnings("unchecked")
        @Override
        public T fromString(final String value) {
            try {
                return (T) IdUtil.fromString(value);
            } catch (Exception e) {
                throw supplier.get();
            }
        }

        /**
         * Convert it to a string.
         *
         * @param value The value to convert.
         * @return The value, converted to a String.
         */
        @Override
        public String toString(final T value) {
            return IdUtil.toString((BigInteger) value);
        }
    }
}
