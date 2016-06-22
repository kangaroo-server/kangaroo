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

package net.krotscheck.features.jackson;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
        ServiceLocator locator = mock(ServiceLocator.class);
        Factory factory = new ObjectMapperFactory(locator);

        Object instance = factory.provide();
        Assert.assertTrue(instance instanceof ObjectMapper);
        factory.dispose(instance);
    }

    /**
     * Assert that the factory creates a functioning Object Mapper.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testProvideObjectMapper() throws Exception {
        ServiceLocator locator = mock(ServiceLocator.class);
        ObjectMapperFactory factory = new ObjectMapperFactory(locator);

        ObjectMapper mapper = factory.provide();
        JsonNode node = mapper.readTree("{}");
        String result = mapper.writeValueAsString(node);

        Assert.assertEquals(result, "{}");
    }

    /**
     * Assert that expected configuration options on the ObjectMapper are set.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testObjectMapperConfig() throws Exception {
        ServiceLocator locator = mock(ServiceLocator.class);
        ObjectMapperFactory factory = new ObjectMapperFactory(locator);

        ObjectMapper mapper = factory.provide();

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
    }

    /**
     * Assert that the object mapper is appropriately disposed of.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testObjectMapperDispose() throws Exception {
        ServiceLocator locator = mock(ServiceLocator.class);
        ObjectMapperFactory factory = new ObjectMapperFactory(locator);
        ObjectMapper mapperSpy = spy(ObjectMapper.class);

        factory.dispose(mapperSpy);

        verifyNoMoreInteractions(mapperSpy);
    }

    /**
     * Assert that modules injected into the context are registered with the
     * Object Mapper.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testModuleRegistration() throws Exception {
        ServiceLocator locator = mock(ServiceLocator.class);
        List<Module> modules = new ArrayList<>();
        modules.add(new JacksonSerializerModule(locator));
        when(locator.getAllServices(Module.class)).thenReturn(modules);

        ObjectMapperFactory factory = new ObjectMapperFactory(locator);
        ObjectMapper mapper = factory.provide();
    }
}
