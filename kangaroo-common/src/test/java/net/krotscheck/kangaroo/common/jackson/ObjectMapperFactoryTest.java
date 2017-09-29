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

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test for our singleton object mapper factory.
 *
 * @author Michael Krotscheck
 */
public class ObjectMapperFactoryTest {

    /**
     * The mapper factory we're testing.
     */
    private static final ObjectMapperFactory FACTORY = new
            ObjectMapperFactory();

    /**
     * Assert that various configuration settings are what we need them to be.
     */
    @Test
    public void testConfigurationSettings() {
        ObjectMapper mapper = FACTORY.get();
        SerializationConfig serializationConfig =
                mapper.getSerializationConfig();
        DeserializationConfig deserializationConfig =
                mapper.getDeserializationConfig();

        // Deserialization feature check
        assertTrue(deserializationConfig
                .isEnabled(DeserializationFeature.READ_ENUMS_USING_TO_STRING));
        assertTrue(deserializationConfig
                .isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
        assertTrue(deserializationConfig
                .isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS));

        assertFalse(deserializationConfig
                .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        // Serialization feature check
        assertTrue(serializationConfig
                .isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING));

        assertFalse(serializationConfig
                .isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES));
    }

    /**
     * Assert that various configuration settings are what we need them to be.
     */
    @Test
    public void testDateFormat() {
        ObjectMapper mapper = FACTORY.get();
        assertTrue(mapper.getDateFormat() instanceof ISO8601DateFormat);
    }

    /**
     * Assert that it's all a singleton.
     */
    @Test
    public void testSingleton() {
        assertSame(FACTORY.get(), FACTORY.get());
    }
}