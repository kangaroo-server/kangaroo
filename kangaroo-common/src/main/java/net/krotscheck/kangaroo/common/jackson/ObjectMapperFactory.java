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

package net.krotscheck.kangaroo.common.jackson;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import net.krotscheck.kangaroo.common.jackson.types.KangarooCustomTypesModule;

import java.util.function.Supplier;

/**
 * Build our object mapper - note that while we adhere to the Jersey2
 * injection SPI here, we are NOT injecting it. That is because this mapper
 * needs to be provided during feature registration, not at resolution time.
 *
 * @author Michael Krotscheck
 */
public final class ObjectMapperFactory implements Supplier<ObjectMapper> {

    /**
     * The system's singleton object mapper.
     */
    private static final ObjectMapper MAPPER;

    static {

        // Create the new object mapper.
        MAPPER = new ObjectMapper();

        // Enable/disable various configuration flags.
        MAPPER.configure(
                DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        MAPPER.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        MAPPER.configure(
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);

        MAPPER.configure(
                SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        MAPPER.configure(
                SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        // Make sure date format is ISO Compliant.
        MAPPER.setDateFormat(new ISO8601DateFormat());

        // Add our annotation introspectors.
        AnnotationIntrospector jaxbIntrospector =
                new JaxbAnnotationIntrospector(MAPPER.getTypeFactory());
        AnnotationIntrospector jacksonIntrospector =
                new JacksonAnnotationIntrospector();
        AnnotationIntrospectorPair combinedIntrospector =
                new AnnotationIntrospectorPair(jacksonIntrospector,
                        jaxbIntrospector);
        MAPPER.setAnnotationIntrospector(combinedIntrospector);

        // Inject our serializers/deserializers.
        MAPPER.registerModule(new KangarooCustomTypesModule());

    }

    /**
     * Build a fully configured object mapper to use for de/serialization.
     *
     * @return The new mapper.
     */
    public ObjectMapper get() {
        return MAPPER;
    }
}
