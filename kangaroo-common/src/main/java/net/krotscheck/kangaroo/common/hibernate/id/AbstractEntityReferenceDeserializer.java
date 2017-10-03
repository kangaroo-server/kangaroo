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


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import net.krotscheck.kangaroo.common.hibernate.entity.AbstractEntity;

import java.io.IOException;

/**
 * A hibernate reference deserializer for generic references - converts
 * incoming license references from their ID's to actual records.
 *
 * @param <T> The managed type to deserialize.
 * @author Michael Krotscheck
 */
public abstract class AbstractEntityReferenceDeserializer
        <T extends AbstractEntity>
        extends StdScalarDeserializer<T> {

    /**
     * Create a new instance of the reference deserializer.
     *
     * @param vc The handled type.
     */
    protected AbstractEntityReferenceDeserializer(final Class<?> vc) {
        super(vc);
    }

    /**
     * When provided with a JSON number, will generate an instance that can be
     * merged with the hibernate context.
     *
     * @param parser  The JSON parser
     * @param context The deserialization context
     * @return A license object populated with the ID.
     * @throws IOException Thrown when parsing fails.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final T deserialize(final JsonParser parser,
                               final DeserializationContext context)
            throws IOException {

        String id = _parseString(parser, context);

        try {
            T instance = (T) handledType().newInstance();
            instance.setId(IdUtil.fromString(id));
            return instance;
        } catch (InstantiationException | IllegalAccessException ie) {
            throw context.mappingException("Cannot instantiate mapped "
                    + "type");
        }
    }
}
