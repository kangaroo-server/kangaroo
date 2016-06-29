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

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A jackson mapper module that collects all serializers and deserializers that
 * were injected into the context.
 *
 * @author Michael Krotscheck
 */
public final class JacksonSerializerModule extends SimpleModule {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(JacksonSerializerModule.class);

    /**
     * Create a new instance of the configuration reader.
     *
     * @param locator The service locator for the injection context.
     */
    @Inject
    public JacksonSerializerModule(final ServiceLocator locator) {
        super("JacksonMapperModule");

        // Register deserializers
        List<StdDeserializer> deserializers =
                locator.getAllServices(StdDeserializer.class);
        for (StdDeserializer deserializer : deserializers) {
            logger.warn(String.format("Registering deserializer for [%s]",
                    deserializer.handledType()));
            addDeserializer(deserializer.handledType(), deserializer);
        }

        // Register serializers
        List<StdSerializer> serializers =
                locator.getAllServices(StdSerializer.class);
        for (StdSerializer serializer : serializers) {
            logger.warn(String.format("Registering serializer for [%s]",
                    serializer.handledType()));
            addSerializer(serializer.handledType(), serializer);
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(JacksonSerializerModule.class)
                    .to(Module.class)
                    .in(Singleton.class);
        }
    }
}
