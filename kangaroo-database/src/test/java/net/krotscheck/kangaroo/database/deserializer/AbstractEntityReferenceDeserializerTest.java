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

package net.krotscheck.kangaroo.database.deserializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import net.krotscheck.kangaroo.database.test.TestChildEntity;
import net.krotscheck.kangaroo.database.test.TestPrivateEntity;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.UUID;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the generic ID deserializer.
 *
 * @author Michael Krotscheck
 */
public final class AbstractEntityReferenceDeserializerTest {

    /**
     * Test generic class method invocation.
     *
     * Using and extending generics compiled in older JDK's creates shadow
     * methods that are nondeterministic. This invokes that method to ensure
     * proper code coverage.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testAbstractConstructor() throws Exception {
        UUID uuid = UUID.randomUUID();
        String id = String.format("\"%s\"", uuid);
        StdScalarDeserializer deserializer =
                new TestDeserializer();

        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        TestChildEntity e = (TestChildEntity)
                deserializer.deserialize(preloadedParser,
                        mock(DeserializationContext.class));

        Assert.assertEquals(uuid, e.getId());
    }

    /**
     * Test simple deserialization.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testDeserializeSimple() throws Exception {
        UUID uuid = UUID.randomUUID();
        String id = String.format("\"%s\"", uuid);
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        TestDeserializer deserializer = new TestDeserializer();
        TestChildEntity e = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, e.getId());
    }

    /**
     * Throw an exception.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = JsonMappingException.class)
    public void testSerializePrivate() throws Exception {
        UUID uuid = UUID.randomUUID();
        String id = String.format("\"%s\"", uuid);
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        TestPrivateDeserializer deserializer = new TestPrivateDeserializer();
        DeserializationContext context = mock(DeserializationContext.class);
        doCallRealMethod().when(context)
                .mappingException(Matchers.anyString());

        deserializer.deserialize(preloadedParser, context);
    }

    /**
     * Test deserialization with no value.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = JsonMappingException.class)
    public void testSerializeNoValue() throws Exception {
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser("\"\"");
        preloadedParser.nextToken(); // Advance to the first value.

        TestPrivateDeserializer deserializer = new TestPrivateDeserializer();
        DeserializationContext context = mock(DeserializationContext.class);
        doCallRealMethod().when(context)
                .mappingException(Matchers.anyString());

        deserializer.deserialize(preloadedParser, context);
    }

    /**
     * Test deserialization with null value.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = JsonMappingException.class)
    public void testSerializeNullValue() throws Exception {
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser("\"null\"");
        preloadedParser.nextToken(); // Advance to the first value.

        TestPrivateDeserializer deserializer = new TestPrivateDeserializer();
        DeserializationContext context = mock(DeserializationContext.class);
        doCallRealMethod().when(context)
                .mappingException(Matchers.anyString());

        deserializer.deserialize(preloadedParser, context);
    }

    /**
     * Test deserializer for the child entity.
     *
     * @author Michael Krotscheck
     */
    public static class TestDeserializer
            extends AbstractEntityReferenceDeserializer<TestChildEntity> {

        /**
         * Create a new deserializer.
         */
        TestDeserializer() {
            super(TestChildEntity.class);
        }
    }

    /**
     * Test deserializer for the private child entity.
     *
     * @author Michael Krotscheck
     */
    public static class TestPrivateDeserializer
            extends AbstractEntityReferenceDeserializer<TestPrivateEntity> {

        /**
         * Create a new deserializer.
         */
        TestPrivateDeserializer() {
            super(TestPrivateEntity.class);
        }
    }

}
