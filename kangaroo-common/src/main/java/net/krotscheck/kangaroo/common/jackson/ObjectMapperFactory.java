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

package net.krotscheck.kangaroo.common.jackson;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A factory that builds object mappers.
 *
 * @author Michael Krotscheck
 */
public final class ObjectMapperFactory implements Factory<ObjectMapper> {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(ObjectMapperFactory.class);

    /**
     * The global service locator.
     */
    private final ServiceLocator locator;

    /**
     * Create a new instance of this factory.
     *
     * @param serviceLocator The injected service locator, used for discovering
     *                       other injected jackson components.
     */
    @Inject
    public ObjectMapperFactory(final ServiceLocator serviceLocator) {
        locator = serviceLocator;
    }

    /**
     * Create an instance of our object mapper.
     *
     * @return The configured and injected object mapper.
     */
    @Override
    public ObjectMapper provide() {

        // Create the new object mapper.
        ObjectMapper mapper = new ObjectMapper();

        // Enable/disable various configuration flags.
        mapper.configure(
                DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        mapper.configure(
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);

        mapper.configure(
                SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(
                SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        // Make sure date format is ISO Compliant.
        mapper.setDateFormat(new ISO8601DateFormat());

        // Add our annotation introspectors.
        AnnotationIntrospector jaxbIntrospector =
                new JaxbAnnotationIntrospector(mapper.getTypeFactory());
        AnnotationIntrospector jacksonIntrospector =
                new JacksonAnnotationIntrospector();
        AnnotationIntrospectorPair combinedIntrospector =
                new AnnotationIntrospectorPair(jacksonIntrospector,
                        jaxbIntrospector);
        mapper.setAnnotationIntrospector(combinedIntrospector);

        // Inject our serializers/deserializers.
        List<Module> modules = locator.getAllServices(Module.class);
        for (Module module : modules) {
            logger.info(String.format(
                    "Registering module with ObjectMapper: %s",
                    module.getModuleName()));
            mapper.registerModule(module);
        }

        return mapper;
    }

    /**
     * Dispose of the object mapper.
     *
     * @param instance The mapper to dispose of.
     */
    @Override
    public void dispose(final ObjectMapper instance) {
        // Do nothing, object mappers have no disposal mechanism.
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(ObjectMapperFactory.class)
                    .to(ObjectMapper.class)
                    .in(Singleton.class);
        }
    }
}
