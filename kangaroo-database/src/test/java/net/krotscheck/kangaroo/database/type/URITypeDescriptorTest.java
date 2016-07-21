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

package net.krotscheck.kangaroo.database.type;

import org.hibernate.HibernateException;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;

/**
 * Tests for the URI Type Descriptor.
 *
 * @author Michael Krotscheck
 */
public final class URITypeDescriptorTest {

    /**
     * Test data.
     */
    private final URI testUri = UriBuilder.fromPath("http://example.com")
            .build();

    /**
     * Assert that we can access this class as its generic type.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGenericConstructor() throws Exception {
        AbstractTypeDescriptor t = new URITypeDescriptor();
        URI result = (URI) t.wrap("http://example.com", null);
        Assert.assertEquals(testUri, result);
    }

    /**
     * Test converting a URI to a string.
     */
    @Test
    public void testToString() {
        String result = URITypeDescriptor.INSTANCE.toString(testUri);
        Assert.assertEquals("http://example.com", result);
    }

    /**
     * Test creating a URI from a string.
     */
    @Test
    public void testFromString() {
        URI result = URITypeDescriptor.INSTANCE
                .fromString("http://example.com");
        Assert.assertEquals(testUri, result);
    }

    /**
     * Test creating a URI from a string.
     */
    @Test(expected = HibernateException.class)
    public void testFromStringNull() {
        URITypeDescriptor.INSTANCE.fromString(null);
    }

    /**
     * Test the unwrap method, null input
     */
    @Test
    public void testUnwrapNull() {
        String result = URITypeDescriptor.INSTANCE.unwrap(null, String.class,
                null);
        Assert.assertNull(result);
    }

    /**
     * Test the unwrap method, String type requested.
     */
    @Test
    public void testUnwrapString() {
        String result = URITypeDescriptor.INSTANCE.unwrap(testUri, String.class,
                null);
        Assert.assertEquals("http://example.com", result);
    }

    /**
     * Test the unwrap method, invalid type requested.
     */
    @Test(expected = HibernateException.class)
    public void testUnwrapInvalid() {
        URITypeDescriptor.INSTANCE.unwrap(testUri, Integer.class, null);
    }

    /**
     * Test the wrap method, null input
     */
    @Test
    public void testWrapNull() {
        URI result =
                URITypeDescriptor.INSTANCE.wrap(null, null);
        Assert.assertNull(result);
    }

    /**
     * Test the wrap method, string requested
     */
    @Test
    public void testWrapString() {
        URI result =
                URITypeDescriptor.INSTANCE.wrap("http://example.com", null);
        Assert.assertEquals(testUri, result);
    }

    /**
     * Test the wrap method, null input
     */
    @Test(expected = HibernateException.class)
    public void testWrapUnknown() {
        URITypeDescriptor.INSTANCE.wrap(1000, null);
    }
}
