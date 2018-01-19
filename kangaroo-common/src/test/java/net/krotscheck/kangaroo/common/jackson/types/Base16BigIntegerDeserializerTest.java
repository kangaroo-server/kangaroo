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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Test string-to-byte deserialization.
 *
 * @author Michael Krotscheck
 */
public final class Base16BigIntegerDeserializerTest {

    /**
     * Assert that we can access this class as its generic type.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGenericConstructor() throws Exception {
        BigInteger id = IdUtil.next();
        String idBody = String.format("\"%s\"", IdUtil.toString(id));
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(idBody);
        preloadedParser.nextToken(); // Advance to the first value.

        JsonDeserializer deserializer = new Base16BigIntegerDeserializer();
        BigInteger deserializedId = (BigInteger)
                deserializer.deserialize(preloadedParser,
                        mock(DeserializationContext.class));

        assertEquals(id, deserializedId);
    }

    /**
     * Assert that deserialization works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void deserialize() throws Exception {
        BigInteger id = IdUtil.next();
        String idBody = String.format("\"%s\"", IdUtil.toString(id));
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(idBody);
        preloadedParser.nextToken(); // Advance to the first value.

        Base16BigIntegerDeserializer deserializer =
                new Base16BigIntegerDeserializer();
        BigInteger deserializedId = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        assertEquals(id, deserializedId);
    }
}
