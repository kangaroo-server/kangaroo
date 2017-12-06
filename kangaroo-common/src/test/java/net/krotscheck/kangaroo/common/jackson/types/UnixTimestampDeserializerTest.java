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
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Test string-to-byte deserialization.
 *
 * @author Michael Krotscheck
 */
public class UnixTimestampDeserializerTest {

    /**
     * Assert that we can access this class as its generic type.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGenericConstructor() throws Exception {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);

        long unixTimestamp = c.getTimeInMillis() / 1000;
        String idBody = String.format("%s", unixTimestamp);

        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(idBody);
        preloadedParser.nextToken(); // Advance to the first value.

        JsonDeserializer deserializer = new UnixTimestampDeserializer();
        Calendar deserialized = (Calendar)
                deserializer.deserialize(preloadedParser,
                        mock(DeserializationContext.class));

        assertEquals(c, deserialized);
    }

    /**
     * Assert that deserialization works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void deserialize() throws Exception {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);

        long unixTimestamp = c.getTimeInMillis() / 1000;
        String idBody = String.format("%s", unixTimestamp);

        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(idBody);
        preloadedParser.nextToken(); // Advance to the first value.

        UnixTimestampDeserializer deserializer = new UnixTimestampDeserializer();
        Calendar deserialized = (Calendar)
                deserializer.deserialize(preloadedParser,
                        mock(DeserializationContext.class));

        assertEquals(c, deserialized);
    }
}
