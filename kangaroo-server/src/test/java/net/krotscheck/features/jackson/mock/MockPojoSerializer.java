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

package net.krotscheck.features.jackson.mock;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.io.IOException;
import javax.inject.Singleton;

/**
 * Mock serializer.
 *
 * @author Michael Krotscheck
 */
public final class MockPojoSerializer extends
        StdSerializer<MockPojo> {

    /**
     * Create a new instance of the deserializer.
     */
    public MockPojoSerializer() {
        super(MockPojo.class);
    }

    /**
     * Test the serialization chain.
     *
     * @param value    The value to serialize.
     * @param jgen     The JSON Generator.
     * @param provider Serialization provider.
     * @throws IOException Not thrown.
     */
    @Override
    public void serialize(final MockPojo value,
                          final JsonGenerator jgen,
                          final SerializerProvider provider)
            throws IOException {

        value.setInvokedSerializer(true);

        jgen.writeStartObject();
        jgen.writeBooleanField("invokedDeserializer",
                value.isInvokedDeserializer());
        jgen.writeBooleanField("invokedSerializer",
                value.isInvokedSerializer());
        jgen.writeBooleanField("serviceData",
                value.isServiceData());
        jgen.writeEndObject();
    }

    /**
     * Bind our serializer into the application scope.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(MockPojoSerializer.class)
                    .to(StdSerializer.class)
                    .in(Singleton.class);
        }
    }
}
