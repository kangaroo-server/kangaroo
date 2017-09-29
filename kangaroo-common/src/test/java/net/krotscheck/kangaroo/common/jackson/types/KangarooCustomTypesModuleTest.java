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

package net.krotscheck.kangaroo.common.jackson.types;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module.SetupContext;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test the jackson module that carries our custom type de/serializers.
 *
 * @author Michael Krotscheck
 */
public final class KangarooCustomTypesModuleTest {

    /**
     * Assert the module name is as expected.
     */
    @Test
    public void getModuleName() {
        KangarooCustomTypesModule module = new KangarooCustomTypesModule();
        assertEquals("KangarooCustomTypesModule", module.getModuleName());
    }

    /**
     * Assert the version name is as expected.
     */
    @Test
    public void version() {
        KangarooCustomTypesModule module = new KangarooCustomTypesModule();
        assertEquals(Version.unknownVersion(), module.version());
    }

    /**
     * Assert that our custom types are registered.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void setupModule() throws Exception {
        KangarooCustomTypesModule module = new KangarooCustomTypesModule();

        SetupContext c = mock(SetupContext.class);
        final ArgumentCaptor<Serializers> serializers =
                ArgumentCaptor.forClass(Serializers.class);
        final ArgumentCaptor<Deserializers> deserializers =
                ArgumentCaptor.forClass(Deserializers.class);

        module.setupModule(c);

        verify(c).addSerializers(serializers.capture());
        verify(c).addDeserializers(deserializers.capture());

        TypeFactory t = TypeFactory.defaultInstance();
        ArrayType byteArrayType = t.constructArrayType(byte.class);

        SimpleSerializers s = (SimpleSerializers) serializers.getValue();
        JsonSerializer byteSerializer =
                s.findSerializer(null, byteArrayType, null);
        assertTrue(byteSerializer instanceof Base16ByteSerializer);

        SimpleDeserializers d = (SimpleDeserializers) deserializers.getValue();
        JsonDeserializer byteDeserializer =
                d.findBeanDeserializer(byteArrayType, null, null);
        assertTrue(byteDeserializer instanceof Base16ByteDeserializer);
    }
}
