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
 *
 */

package net.krotscheck.kangaroo.database.type;

import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.type.DiscriminatorType;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * Tests for the custom URI type.
 *
 * @author Michael Krotscheck
 */
public final class URITypeTest {

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
        DiscriminatorType t = new URIType();
        URI uri = (URI) t.stringToObject("http://example.com");
        Assert.assertEquals(testUri, uri);
    }

    /**
     * Assert that we should register this type.
     */
    @Test
    public void testRegisterUnderJavaType() {
        URIType t = new URIType();
        Assert.assertTrue(t.registerUnderJavaType());
    }

    /**
     * Assert that we can convert a URI to a string.
     */
    @Test
    public void testToString() {
        URIType t = new URIType();
        String uriString = t.toString(testUri);
        Assert.assertEquals("http://example.com", uriString);
    }

    /**
     * Assert that we can convert a string into a URI.
     *
     * @throws Exception Thrown if something goes sideways.
     */
    @Test
    public void testStringToObject() throws Exception {
        URIType t = new URIType();
        URI uri = t.stringToObject("http://example.com");
        Assert.assertEquals(testUri, uri);
    }

    /**
     * Test converting an object into SQL.
     *
     * @throws Exception Thrown if something goes sideways.
     */
    @Test
    public void testObjectToSQLString() throws Exception {
        URIType t = new URIType();
        String sql = t.objectToSQLString(testUri, new MySQL5Dialect());
        Assert.assertEquals("'http://example.com'", sql);
    }

    /**
     * Assert that the name is unique.
     */
    @Test
    public void testGetName() {
        URIType t = new URIType();
        Assert.assertEquals("uri", t.getName());
    }
}
