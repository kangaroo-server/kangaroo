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

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.StringType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import java.net.URI;

/**
 * Custom hibernate type that permits storing URI's into varchar columns.
 *
 * @author Michael Krotscheck
 */
public final class URIType
        extends AbstractSingleColumnStandardBasicType<URI>
        implements DiscriminatorType<URI> {

    /**
     * Create a new instance of this user type.
     */
    public URIType() {
        super(VarcharTypeDescriptor.INSTANCE, URITypeDescriptor.INSTANCE);
    }


    /**
     * Whether to register this type.
     *
     * @return true
     */
    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    /**
     * Convert the URI to a string.
     *
     * @param value The URI as a value.
     * @return The URI as a string.
     */
    @Override
    public String toString(final URI value) {
        return URITypeDescriptor.INSTANCE.toString(value);
    }

    /**
     * Convert the value from the mapping file to a Java object.
     *
     * @param xml the value of <tt>discriminator-value</tt> or
     *            <tt>unsaved-value</tt> attribute
     * @return The converted value of the string representation.
     * @throws Exception Indicates a problem converting from the string
     */
    @Override
    public URI stringToObject(final String xml) throws Exception {
        return URITypeDescriptor.INSTANCE.fromString(xml);
    }

    /**
     * Convert the value into a string representation, suitable for embedding
     * in
     * an SQL statement as a
     * literal.
     *
     * @param value   The value to convert
     * @param dialect The SQL dialect
     * @return The value's string representation
     * @throws Exception Indicates an issue converting the value to literal
     *                   string.
     */
    @Override
    public String objectToSQLString(final URI value,
                                    final Dialect dialect)
            throws Exception {
        return StringType.INSTANCE.objectToSQLString(toString(value), dialect);
    }

    /**
     * Returns the abbreviated name of the type.
     *
     * @return String the Hibernate type name
     */
    @Override
    public String getName() {
        return "uri";
    }
}
