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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.krotscheck.kangaroo.common.jackson.mock.MockPojo;
import net.krotscheck.kangaroo.common.jackson.mock.MockPojoDeserializer;
import net.krotscheck.kangaroo.common.jackson.mock.MockPojoSerializer;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for the jackson serializer module.
 *
 * @author Michael Krotscheck
 */
public final class JacksonSerializerModuleTest {

    /**
     * Assert that deserializers and serializers are detected and added to the
     * module.
     *
     * @throws Exception Throws an exception.
     */
    @Test
    public void testCollectsDeSerializers() throws Exception {
        InjectionManager injector = Injections.createInjectionManager();

        List<StdDeserializer> deserializers = new ArrayList<>();
        List<StdSerializer> serializers = new ArrayList<>();

        // Build our deserializer collections
        injector.register(new MockPojoDeserializer.Binder());
        injector.register(new MockPojoSerializer.Binder());

        JacksonSerializerModule m =
                new JacksonSerializerModule(injector);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(m);

        // Assert that the mapper can deserialize this method.
        JavaType mockType = mapper.constructType(MockPojo.class);
        Assert.assertTrue(mapper.canDeserialize(mockType));
        Assert.assertTrue(mapper.canSerialize(MockPojo.class));

        // Serialize the value, make sure it passes though the serializer.
        MockPojo original = new MockPojo();
        String json = mapper.writeValueAsString(original);
        Assert.assertTrue(original.isInvokedSerializer());

        // Deserialize the value, make sure it passes through the deserializer.
        MockPojo deserialized = mapper.readValue(json, MockPojo.class);
        Assert.assertTrue(deserialized.isInvokedDeserializer());

        injector.shutdown();
    }
}
