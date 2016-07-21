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
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;

/**
 * Descriptor for {@link URI} handling.
 *
 * @author Steve Ebersole
 */
public final class URITypeDescriptor extends AbstractTypeDescriptor<URI> {

    /**
     * Singleton instance for convenience access.
     */
    public static final URITypeDescriptor INSTANCE = new URITypeDescriptor();

    /**
     * Constructor.
     */
    public URITypeDescriptor() {
        super(URI.class);
    }

    /**
     * Convert the value to a string.
     *
     * @param value Value to convert.
     * @return Value as a string.
     */
    public String toString(final URI value) {
        return value.toString();
    }

    /**
     * Create a URI from a string.
     *
     * @param string The string.
     * @return The URI of that string.
     */
    public URI fromString(final String string) {
        try {
            return UriBuilder.fromPath(string).build();
        } catch (Exception e) {
            throw new HibernateException(
                    "Unable to convert string [" + string + "] to URI : " + e);
        }
    }

    /**
     * Unwrap an instance of our handled Java type into the requested type.
     *
     * @param value   The value to unwrap
     * @param type    The type as which to unwrap
     * @param options The options
     * @param <X>     The conversion type.
     * @return The unwrapped value.
     */
    @SuppressWarnings({"unchecked"})
    public <X> X unwrap(final URI value,
                        final Class<X> type,
                        final WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (String.class.isAssignableFrom(type)) {
            return (X) toString(value);
        }
        throw unknownUnwrap(type);
    }

    /**
     * Wrap a value as our handled Java type.
     *
     * @param value   The value to wrap.
     * @param options The options
     * @param <X>     The conversion type.
     * @return The wrapped value.
     */
    public <X> URI wrap(final X value, final WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (String.class.isInstance(value)) {
            return fromString((String) value);
        }
        throw unknownWrap(value.getClass());
    }
}
