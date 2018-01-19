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

import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;

/**
 * Unit test for the BigInteger-to-byte[] converter.
 *
 * @author Michael Krotscheck
 */
public final class BigIntegerTypeTest {

    /**
     * Assert that we map to the correct types.
     */
    @Test
    public void sqlTypes() {
        BigIntegerType type = new BigIntegerType();

        int[] types = type.sqlTypes();
        assertEquals(1, types.length);
        assertEquals(Types.BINARY, types[0]);
    }

    /**
     * Assert that the converted type is a BigInteger.
     */
    @Test
    public void returnedClass() {
        BigIntegerType type = new BigIntegerType();

        assertEquals(BigInteger.class, type.returnedClass());
    }

    /**
     * Assert that comparison works.
     */
    @Test
    public void equals() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = IdUtil.next();
        BigInteger two = new BigInteger(one.toByteArray());
        BigInteger three = IdUtil.next();

        // Basic comparison
        assertTrue(type.equals(one, two));
        assertFalse(type.equals(three, two));
        assertFalse(type.equals(two, three));
        assertTrue(type.equals(null, null));
        assertFalse(type.equals(null, two));
        assertFalse(type.equals(two, null));
    }

    /**
     * Assert that hashcode is passed through to the underlying instance.
     */
    @Test
    public void testHashCode() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = IdUtil.next();
        assertEquals(one.hashCode(), type.hashCode(one));
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
        BigInteger one = IdUtil.next();

        Mockito.doReturn(one.toByteArray())
                .when(rs).getBytes(names[0]);
        BigInteger result = (BigInteger)
                type.nullSafeGet(rs, names, null, null);

        assertEquals(one, result);
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

        assertNull(result);
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
        BigInteger one = BigInteger.ONE;
        type.nullSafeSet(st, one, 0, null);

        // Expected value is a left-padded byte array.
        byte[] zerofill = new byte[16];
        byte[] expected = one.toByteArray();
        // Prefix zeros if necessary...
        System.arraycopy(expected, 0, zerofill,
                zerofill.length - expected.length,
                expected.length);
        expected = zerofill;

        Mockito.verify(st, times(1))
                .setBytes(0, expected);
    }

    /**
     * Assert that a 'short' byte string is appropriately zero padded.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void nullSafeShort() throws Exception {
        BigIntegerType type = new BigIntegerType();
        PreparedStatement st = Mockito.mock(PreparedStatement.class);
        BigInteger one = BigInteger.TEN;
        type.nullSafeSet(st, one, 0, null);
        Mockito.verify(st, times(1))
                .setBytes(0, new byte[]{
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10
                });
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
        BigInteger one = IdUtil.next();
        assertSame(one, type.deepCopy(one));
    }

    /**
     * Assert that ismutable is false.
     */
    @Test
    public void isMutable() {
        BigIntegerType type = new BigIntegerType();
        assertFalse(type.isMutable());
    }

    /**
     * Assert that a BigInteger is disassembled into a byte[].
     */
    @Test
    public void disassemble() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = IdUtil.next();
        byte[] result = (byte[]) type.disassemble(one);
        assertArrayEquals(one.toByteArray(), result);
    }

    /**
     * Assert that a byte[] can be converted into a BigInteger.
     */
    @Test
    public void assemble() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = IdUtil.next();
        BigInteger result = (BigInteger) type.assemble(one.toByteArray(), null);
        assertTrue(one.equals(result));
    }

    /**
     * This type is immutable, replace should only return the first response.
     */
    @Test
    public void replace() {
        BigIntegerType type = new BigIntegerType();

        BigInteger one = IdUtil.next();
        BigInteger two = IdUtil.next();
        BigInteger result = (BigInteger) type.replace(one, two, null);

        assertSame(one, result);
    }
}
