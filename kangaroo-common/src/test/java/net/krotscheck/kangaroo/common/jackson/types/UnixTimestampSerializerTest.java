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
import org.junit.Test;

import java.util.Calendar;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the timestamp serializer.
 *
 * @author Michael Krotscheck
 */
public final class UnixTimestampSerializerTest {

    /**
     * Assert that we can access this class as its generic type.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGenericConstructor() throws Exception {
        Calendar c = Calendar.getInstance();
        long unixTimestamp = c.getTimeInMillis() / 1000;

        JsonSerializer serializer = new UnixTimestampSerializer();
        JsonGenerator generator = mock(JsonGenerator.class);

        serializer.serialize(c, generator, null);

        verify(generator).writeNumber(unixTimestamp);
    }

    /**
     * Assert that deserialization works.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void deserialize() throws Exception {
        Calendar c = Calendar.getInstance();
        long unixTimestamp = c.getTimeInMillis() / 1000;

        UnixTimestampSerializer serializer = new UnixTimestampSerializer();
        JsonGenerator generator = mock(JsonGenerator.class);

        serializer.serialize(c, generator, null);

        verify(generator).writeNumber(unixTimestamp);
    }

}
