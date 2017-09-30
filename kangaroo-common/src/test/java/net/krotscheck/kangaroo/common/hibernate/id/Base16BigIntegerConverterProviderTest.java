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

package net.krotscheck.kangaroo.common.hibernate.id;

import org.junit.Test;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ext.ParamConverter;
import java.lang.annotation.Annotation;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for the data provider.
 *
 * @author Michael Krotscheck
 */
public final class Base16BigIntegerConverterProviderTest {

    /**
     * The converter under test.
     */
    private Base16BigIntegerConverterProvider converterProvider =
            new Base16BigIntegerConverterProvider();

    /**
     * Path param annotation for testing.
     */
    private Annotation pathAnnotation = new PathParam() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return PathParam.class;
        }

        @Override
        public String value() {
            return null;
        }
    };
    /**
     * Query param annotation for testing.
     */
    private Annotation queryAnnotation = new QueryParam() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return QueryParam.class;
        }

        @Override
        public String value() {
            return null;
        }
    };

    /**
     * Assert that they only work for BigIntegers.
     */
    @Test
    public void testBigIntegerOnly() {
        Annotation[] annotations = new Annotation[]{
                pathAnnotation, queryAnnotation};
        assertNotNull(converterProvider
                .getConverter(BigInteger.class, null, annotations));

        assertNull(converterProvider
                .getConverter(Integer.class, null, annotations));
        assertNull(converterProvider
                .getConverter(String.class, null, annotations));
        assertNull(converterProvider
                .getConverter(Byte.class, null, annotations));
        assertNull(converterProvider
                .getConverter(Double.class, null, annotations));
        assertNull(converterProvider
                .getConverter(Number.class, null, annotations));
    }

    /**
     * Assert that we have no annotations.
     */
    @Test
    public void testNoAnnotation() {
        Annotation[] annotations = new Annotation[]{};
        assertNull(converterProvider
                .getConverter(BigInteger.class, null, annotations));
    }

    /**
     * Assert that they work for QueryParam.
     */
    @Test
    public void testOnlyQueryParam() {
        Annotation[] annotations = new Annotation[]{queryAnnotation};
        assertNotNull(converterProvider
                .getConverter(BigInteger.class, null, annotations));
    }

    /**
     * Assert that they work for PathParam.
     */
    @Test
    public void testOnlyPathParam() {
        Annotation[] annotations = new Annotation[]{pathAnnotation};
        assertNotNull(converterProvider
                .getConverter(BigInteger.class, null, annotations));
    }

    /**
     * The biginteger-to-string converter.
     */
    @Test
    public void testConvertBigInteger() {
        Annotation[] annotations = new Annotation[]{pathAnnotation};

        ParamConverter converter = converterProvider
                .getConverter(BigInteger.class, null, annotations);

        BigInteger id = IdUtil.next();
        String idString = converter.toString(id);
        assertEquals(idString, IdUtil.toString(id));
    }

    /**
     * The string-to-biginteger converter.
     */
    @Test
    public void testConvertString() {
        Annotation[] annotations = new Annotation[]{pathAnnotation};

        ParamConverter converter = converterProvider
                .getConverter(BigInteger.class, null, annotations);

        BigInteger id = IdUtil.next();
        BigInteger converted = (BigInteger)
                converter.fromString(IdUtil.toString(id));
        assertEquals(id, converted);
    }
}
