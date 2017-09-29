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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test string-to-byte deserialization.
 *
 * @author Michael Krotscheck
 */
public class Base16ByteSerializerTest {

    /**
     * Assert that we can access this class as its generic type.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGenericConstructor() throws Exception {
        byte[] id = IdUtil.next();
        String idString = IdUtil.toString(id);

        JsonSerializer serializer = new Base16ByteSerializer();
        JsonGenerator generator = mock(JsonGenerator.class);

        serializer.serialize(id, generator, null);

        verify(generator).writeString(idString);
    }

    /**
     * Assert that deserialization works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void deserialize() throws Exception {
        byte[] id = IdUtil.next();
        String idString = IdUtil.toString(id);

        Base16ByteSerializer serializer = new Base16ByteSerializer();
        JsonGenerator generator = mock(JsonGenerator.class);

        serializer.serialize(id, generator, null);

        verify(generator).writeString(idString);
    }
}