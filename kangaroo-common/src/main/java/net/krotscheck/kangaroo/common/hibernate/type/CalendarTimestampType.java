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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.TimeZone;


/**
 * Custom hibernate type that permits storing Calendar's into varchar columns.
 *
 * @author Michael Krotscheck
 */
public final class CalendarTimestampType implements UserType {

    /**
     * Return the SQL type codes for the columns mapped by this type. The
     * codes are defined on <tt>java.sql.Types</tt>.
     *
     * @return int[] the typecodes
     * @see Types
     */
    @Override
    public int[] sqlTypes() {
        return new int[]{Types.BIGINT};
    }

    /**
     * The class returned by <tt>nullSafeGet()</tt>.
     *
     * @return Class
     */
    @Override
    public Class returnedClass() {
        return Calendar.class;
    }

    /**
     * Compare two instances of the class mapped by this type for persistence
     * "equality". Equality of the persistent state.
     *
     * @param x Left
     * @param y Right
     * @return boolean True if they are equal.
     */
    @Override
    public boolean equals(final Object x,
                          final Object y) throws HibernateException {
        if (x == null) {
            return y == null;
        }
        return x.equals(y);
    }

    /**
     * Get a hashcode for the instance, consistent with persistence "equality".
     *
     * @param x The object to hash.
     */
    @Override
    public int hashCode(final Object x) throws HibernateException {
        return x.hashCode();
    }

    /**
     * Retrieve an instance of the mapped class from a JDBC resultset.
     * Implementors should handle possibility of null values.
     *
     * @param rs      A JDBC result set
     * @param names   The column names
     * @param session The session implementation.
     * @param owner   the containing entity  @return Object
     * @throws HibernateException Thrown if hibernate throws up.
     * @throws SQLException       Thrown if the type is not mapped properly.
     */
    @Override
    public Object nullSafeGet(final ResultSet rs,
                              final String[] names,
                              final SharedSessionContractImplementor session,
                              final Object owner)
            throws HibernateException, SQLException {
        // Read the value as a string, so we can check for null.
        String timeInMillisStr = rs.getString(names[0]);
        if (timeInMillisStr == null) {
            return null;
        }
        Long timeInMillis = Long.valueOf(timeInMillisStr);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(timeInMillis);
        return calendar;
    }

    /**
     * Write an instance of the mapped class to a prepared statement.
     * Implementors should handle possibility of null values. A multi-column
     * type should be written to parameters starting from <tt>index</tt>.
     *
     * @param st      A JDBC prepared statement.
     * @param value   The object to write.
     * @param index   Statement parameter index.
     * @param session The session implementation.
     * @throws HibernateException Thrown if hibernate throws up.
     * @throws SQLException       Thrown if the type is not mapped properly.
     */
    @Override
    public void nullSafeSet(final PreparedStatement st,
                            final Object value,
                            final int index,
                            final SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value != null) {
            Calendar calendar = (Calendar) value;
            st.setLong(index, calendar.getTimeInMillis());
        } else {
            st.setNull(index, Types.BIGINT);
        }
    }

    /**
     * Return a deep copy of the persistent state, stopping at entities and at
     * collections. It is not necessary to copy immutable objects, or null
     * values, in which case it is safe to simply return the argument.
     *
     * @param value the object to be cloned, which may be null
     * @return Object a copy
     */
    @Override
    public Object deepCopy(final Object value) throws HibernateException {
        return value;
    }

    /**
     * Are objects of this type mutable?
     *
     * @return boolean
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * Transform the object into its cacheable representation. At the very
     * least this method should perform a deep copy if the type is mutable.
     * That may not be enough for some implementations, however; for example,
     * associations must be cached as identifier values. (optional operation)
     *
     * @param value The object to be cached
     * @return A cachable representation of the object
     * @throws HibernateException Thrown if hibernate throws up.
     */
    @Override
    public Serializable disassemble(final Object value)
            throws HibernateException {
        Calendar calendar = (Calendar) value;
        return calendar.getTimeInMillis();
    }

    /**
     * Reconstruct an object from the cacheable representation. At the very
     * least this method should perform a deep copy if the type is mutable.
     * (optional operation)
     *
     * @param cached The object to be cached.
     * @param owner  The owner of the cached object.
     * @return A reconstructed object from the cachable representation.
     * @throws HibernateException Thrown if hibernate throws up.
     */
    @Override
    public Object assemble(final Serializable cached,
                           final Object owner) throws HibernateException {
        Long timeInMillis = (Long) cached;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(timeInMillis);
        return calendar;
    }

    /**
     * During merge, replace the existing (target) value in the entity we are
     * merging to with a new (original) value from the detached entity we are
     * merging. For immutable objects, or null values, it is safe to simply
     * return the first parameter. For mutable objects, it is safe to return
     * a copy of the first parameter. For objects with component values, it
     * might make sense to recursively replace component values.
     *
     * @param original the value from the detached entity being merged.
     * @param target   the value in the managed entity.
     * @param owner    The owning entity.
     * @return the value to be merged
     */
    @Override
    public Object replace(final Object original,
                          final Object target,
                          final Object owner)
            throws HibernateException {
        return original;
    }
}
