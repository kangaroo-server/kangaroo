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

package net.krotscheck.kangaroo.common.jackson.mock;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.io.IOException;
import javax.inject.Singleton;

/**
 * A test deserializer.
 *
 * @author Michael Krotscheck
 */
public final class MockPojoDeserializer extends
        StdDeserializer<MockPojo> {

    /**
     * Create a new instance of the deserializer.
     */
    public MockPojoDeserializer() {
        super(MockPojo.class);
    }

    /**
     * Test the Deserialization Chain.
     *
     * @param p    Parsed used for reading JSON content
     * @param ctxt Context that can be used to access information about this
     *             deserialization activity.
     * @return Deserialized value
     */
    @Override
    public MockPojo deserialize(final JsonParser p,
                                final DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        MockPojo pojo = new MockPojo();
        JsonNode node = p.readValueAsTree();

        pojo.setInvokedDeserializer(
                node.get("invokedDeserializer").asBoolean()
        );
        pojo.setInvokedSerializer(
                node.get("invokedSerializer").asBoolean()
        );
        pojo.setServiceData(
                node.get("serviceData").asBoolean()
        );

        pojo.setInvokedDeserializer(true);

        return pojo;
    }

    /**
     * Bind our deserializer into the binding scope.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(MockPojoDeserializer.class)
                    .to(StdDeserializer.class)
                    .in(Singleton.class);
        }
    }
}
