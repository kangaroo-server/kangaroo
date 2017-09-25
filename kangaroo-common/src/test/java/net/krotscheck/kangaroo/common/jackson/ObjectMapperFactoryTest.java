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

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Supplier;

/**
 * Unit tests for the ObjectMapperFactory.
 *
 * @author Michael Krotscheck
 */
public final class ObjectMapperFactoryTest {

    /**
     * Assert that the generic interface works as expected.
     *
     * @throws Exception Should not be thrown.
     * @see <a href="https://sourceforge.net/p/cobertura/bugs/92/">https://sourceforge.net/p/cobertura/bugs/92/</a>
     */
    @Test
    public void testGenericInterface() throws Exception {
        InjectionManager injector = Injections.createInjectionManager();
        Supplier factory = new ObjectMapperFactory(injector);

        Object instance = factory.get();
        Assert.assertTrue(instance instanceof ObjectMapper);

        injector.shutdown();
    }

    /**
     * Assert that the factory creates a functioning Object Mapper.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testProvideObjectMapper() throws Exception {
        InjectionManager injector = Injections.createInjectionManager();
        ObjectMapperFactory factory = new ObjectMapperFactory(injector);

        ObjectMapper mapper = factory.get();
        JsonNode node = mapper.readTree("{}");
        String result = mapper.writeValueAsString(node);

        Assert.assertEquals(result, "{}");

        injector.shutdown();
    }

    /**
     * Assert that expected configuration options on the ObjectMapper are set.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testObjectMapperConfig() throws Exception {
        InjectionManager injector = Injections.createInjectionManager();
        ObjectMapperFactory factory = new ObjectMapperFactory(injector);

        ObjectMapper mapper = factory.get();

        // Test the Deserialization configuration
        DeserializationConfig dConfig = mapper.getDeserializationConfig();
        Assert.assertTrue(dConfig
                .isEnabled(DeserializationFeature.READ_ENUMS_USING_TO_STRING));
        Assert.assertTrue(dConfig
                .isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
        Assert.assertTrue(dConfig
                .isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS));
        Assert.assertFalse(dConfig
                .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        // Test the Serialization configuration
        SerializationConfig sConfig = mapper.getSerializationConfig();
        Assert.assertTrue(sConfig
                .isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING));
        Assert.assertFalse(sConfig
                .isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES));

        injector.shutdown();
    }

    /**
     * Assert that modules injected into the context are registered with the
     * Object Mapper.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testModuleRegistration() throws Exception {
        InjectionManager injector = Injections.createInjectionManager();

        injector.register(new JacksonSerializerModule.Binder());

        ObjectMapperFactory factory = new ObjectMapperFactory(injector);
        factory.get();
    }
}
