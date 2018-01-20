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

import org.junit.Test;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;

/**
 * Unit test for the calendar-to-timestamp converter.
 *
 * @author Michael Krotscheck
 */
public final class CalendarTimestampTypeTest {

    /**
     * Assert that we map to the correct types.
     */
    @Test
    public void sqlTypes() {
        CalendarTimestampType type = new CalendarTimestampType();

        int[] types = type.sqlTypes();
        assertEquals(1, types.length);
        assertEquals(Types.BIGINT, types[0]);
    }

    /**
     * Assert that the converted type is a calendar.
     */
    @Test
    public void returnedClass() {
        CalendarTimestampType type = new CalendarTimestampType();

        assertEquals(Calendar.class, type.returnedClass());
    }

    /**
     * Assert that comparison works.
     */
    @Test
    public void equals() {
        CalendarTimestampType type = new CalendarTimestampType();

        Calendar one = Calendar.getInstance();
        Calendar two = (Calendar) one.clone();
        Calendar three = Calendar.getInstance();
        three.add(Calendar.MINUTE, 1);

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
        CalendarTimestampType type = new CalendarTimestampType();

        Calendar one = Calendar.getInstance();
        assertEquals(one.hashCode(), type.hashCode(one));
    }

    /**
     * Assert that we can retrieve the calendar.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void nullSafeGet() throws Exception {
        CalendarTimestampType type = new CalendarTimestampType();
        String[] names = new String[]{"test"};
        ResultSet rs = Mockito.mock(ResultSet.class);
        Calendar one = Calendar.getInstance();
        Mockito.doReturn(String.valueOf(one.getTimeInMillis()))
                .when(rs).getString(names[0]);
        Calendar result = (Calendar)
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
        CalendarTimestampType type = new CalendarTimestampType();
        String[] names = new String[]{"test"};
        ResultSet rs = Mockito.mock(ResultSet.class);
        Mockito.doReturn(null).when(rs).getString(names[0]);
        Calendar result = (Calendar)
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
        CalendarTimestampType type = new CalendarTimestampType();
        PreparedStatement st = Mockito.mock(PreparedStatement.class);
        Calendar one = Calendar.getInstance();
        type.nullSafeSet(st, one, 0, null);
        Mockito.verify(st, times(1))
                .setLong(0, one.getTimeInMillis());
    }

    /**
     * Assert that setting with a null value sets null.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void nullSafeSetNull() throws Exception {
        CalendarTimestampType type = new CalendarTimestampType();
        PreparedStatement st = Mockito.mock(PreparedStatement.class);
        type.nullSafeSet(st, null, 0, null);
        Mockito.verify(st, times(1)).setNull(0, Types.BIGINT);
    }

    /**
     * Assert that deepCopy returns the same (immutable) instance.
     */
    @Test
    public void deepCopy() {
        CalendarTimestampType type = new CalendarTimestampType();
        Calendar one = Calendar.getInstance();
        assertSame(one, type.deepCopy(one));
    }

    /**
     * Assert that ismutable is false.
     */
    @Test
    public void isMutable() {
        CalendarTimestampType type = new CalendarTimestampType();
        assertFalse(type.isMutable());
    }

    /**
     * Assert that a calendar is disassembled into a long.
     */
    @Test
    public void disassemble() {
        CalendarTimestampType type = new CalendarTimestampType();

        Calendar one = Calendar.getInstance();
        long result = (long) type.disassemble(one);
        assertEquals(one.getTimeInMillis(), result);
    }

    /**
     * Assert that a long can be converted into a calendar.
     */
    @Test
    public void assemble() {
        CalendarTimestampType type = new CalendarTimestampType();

        Calendar one = Calendar.getInstance();
        Calendar result = (Calendar) type.assemble(one.getTimeInMillis(), null);
        assertTrue(one.equals(result));
    }

    /**
     * This type is immutable, replace should only return the first response.
     */
    @Test
    public void replace() {
        CalendarTimestampType type = new CalendarTimestampType();

        Calendar one = Calendar.getInstance();
        Calendar two = Calendar.getInstance();
        Calendar result = (Calendar) type.replace(one, two, null);

        assertSame(one, result);
    }
}
