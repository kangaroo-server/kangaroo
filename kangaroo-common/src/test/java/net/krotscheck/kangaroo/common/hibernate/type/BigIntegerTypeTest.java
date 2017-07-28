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

package net.krotscheck.kangaroo.common.hibernate.type;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Random;

import static org.mockito.Mockito.times;

/**
 * Unit test for the Bigint-to-byte[] field converter.
 *
 * @author Michael Krotscheck
 */
public class BigIntegerTypeTest {

    /**
     * Random byte generator.
     */
    private static final Random RND = new Random();

    /**
     * Assert that we map to the correct types.
     */
    @Test
    public void sqlTypes() {
        BigIntegerType type = new BigIntegerType();

        int[] types = type.sqlTypes();
        Assert.assertEquals(1, types.length);
        Assert.assertEquals(Types.BINARY, types[0]);
    }

    /**
     * Assert that the converted type is a BigInteger.
     */
    @Test
    public void returnedClass() {
        BigIntegerType type = new BigIntegerType();

        Assert.assertEquals(BigInteger.class, type.returnedClass());
    }

    /**
     * Assert that comparison works.
     */
    @Test
    public void equals() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = new BigInteger(16, RND);
        BigInteger two = new BigInteger(one.toByteArray());
        BigInteger three = new BigInteger(16, RND);

        // Basic comparison
        Assert.assertTrue(type.equals(one, two));
        Assert.assertFalse(type.equals(three, two));
        Assert.assertFalse(type.equals(two, three));
        Assert.assertTrue(type.equals(null, null));
        Assert.assertFalse(type.equals(null, two));
        Assert.assertFalse(type.equals(two, null));
    }

    /**
     * Assert that hashcode is passed through to the underlying instance.
     */
    @Test
    public void testHashCode() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = new BigInteger(16, RND);
        Assert.assertEquals(one.hashCode(), type.hashCode(one));
    }

    /**
     * Assert that we can retrieve the BigInteger.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void nullSafeGet() throws Exception {
        BigIntegerType type = new BigIntegerType();
        String[] names = new String[]{"test"};
        ResultSet rs = Mockito.mock(ResultSet.class);

        BigInteger one = new BigInteger(16, RND);
        Mockito.doReturn(one.toByteArray()).when(rs).getBytes(names[0]);
        BigInteger result = (BigInteger)
                type.nullSafeGet(rs, names, null, null);

        Assert.assertEquals(one, result);
    }

    /**
     * Assert that a null value returns null.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void nullSafeGetNull() throws Exception {
        BigIntegerType type = new BigIntegerType();
        String[] names = new String[]{"test"};
        ResultSet rs = Mockito.mock(ResultSet.class);
        Mockito.doReturn(null).when(rs).getString(names[0]);
        BigInteger result = (BigInteger)
                type.nullSafeGet(rs, names, null, null);

        Assert.assertNull(result);
    }

    /**
     * Assert that we can set a value.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void nullSafeSet() throws Exception {
        BigIntegerType type = new BigIntegerType();
        PreparedStatement st = Mockito.mock(PreparedStatement.class);
        BigInteger one = new BigInteger(16, RND);
        type.nullSafeSet(st, one, 0, null);
        Mockito.verify(st, times(1))
                .setBytes(0, one.toByteArray());
    }

    /**
     * Assert that setting with a null value sets null.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void nullSafeSetNull() throws Exception {
        BigIntegerType type = new BigIntegerType();
        PreparedStatement st = Mockito.mock(PreparedStatement.class);
        type.nullSafeSet(st, null, 0, null);
        Mockito.verify(st, times(1)).setNull(0, Types.BINARY);
    }

    /**
     * Assert that deepCopy returns the same (immutable) instance.
     */
    @Test
    public void deepCopy() {
        BigIntegerType type = new BigIntegerType();
        BigInteger one = new BigInteger(16, RND);
        Assert.assertSame(one, type.deepCopy(one));
    }

    /**
     * Assert that ismutable is false.
     */
    @Test
    public void isMutable() {
        BigIntegerType type = new BigIntegerType();
        Assert.assertFalse(type.isMutable());
    }

    /**
     * Assert that a BigInteger is disassembled into a long.
     */
    @Test
    public void disassemble() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = new BigInteger(16, RND);
        String result = (String) type.disassemble(one);
        Assert.assertEquals(one.toString(16), result);
    }

    /**
     * Assert that a long can be converted into a BigInteger.
     */
    @Test
    public void assemble() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = new BigInteger(16, RND);
        BigInteger result = (BigInteger) type.assemble(one.toString(16),
                null);
        Assert.assertTrue(one.equals(result));
    }

    /**
     * This type is immutable, replace should only return the first response.
     */
    @Test
    public void replace() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = new BigInteger(16, RND);
        BigInteger two = new BigInteger(16, RND);
        BigInteger result = (BigInteger) type.replace(one, two, null);

        Assert.assertSame(one, result);
    }

}