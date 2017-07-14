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

package net.krotscheck.kangaroo.test.hk2;

import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

/**
 * A simple iterable provider, for testing purposes.
 *
 * @author Michael Krotscheck
 */
public final class SimpleIterableProvider<T> implements IterableProvider<T> {

    /**
     * The content provided by this provider.
     */
    private final List<T> content;

    /**
     * The content to wrap.
     *
     * @param content A list of content to wrap.
     */
    public SimpleIterableProvider(final List<T> content) {
        this.content = content;
    }

    /**
     * Return the service handle (usually not used).
     *
     * @return null
     */
    @Override
    public ServiceHandle<T> getHandle() {
        return null;
    }

    /**
     * Return the size of the content.
     *
     * @return The size of the content.
     */
    @Override
    public int getSize() {
        return content.size();
    }

    /**
     * Do nothing.
     *
     * @param name Named subselector.
     * @return null
     */
    @Override
    public IterableProvider<T> named(final String name) {
        return null;
    }

    /**
     * Do nothing.
     *
     * @param type Typed subselector.
     * @return null
     */
    @Override
    public <U> IterableProvider<U> ofType(final Type type) {
        return null;
    }

    /**
     * Do nothing.
     *
     * @param qualifiers Annotation subselector.
     * @return null
     */
    @Override
    public IterableProvider<T> qualifiedWith(final Annotation... qualifiers) {
        return null;
    }

    /**
     * Do nothing.
     *
     * @return null
     */
    @Override
    public Iterable<ServiceHandle<T>> handleIterator() {
        return null;
    }

    /**
     * Return an iterator.
     *
     * @return Content iterator.
     */
    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }

    /**
     * Get an instance from the iterator.
     *
     * @return
     */
    @Override
    public T get() {
        return content.iterator().next();
    }
}
