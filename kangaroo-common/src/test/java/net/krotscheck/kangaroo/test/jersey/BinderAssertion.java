/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.test.jersey;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Binder testing utility, to assert that certain contracts are satisfied by
 * a specific binder.
 */
public final class BinderAssertion {

    /**
     * Utility class, private constructor.
     */
    private BinderAssertion() {
    }

    /**
     * Assert that a binder contains the provided types.
     *
     * @param b             The binder to test.
     * @param expectedTypes The types to expect.
     */
    public static void assertBinderContains(final AbstractBinder b,
                                            final Class... expectedTypes) {
        List<Binding> bindings = new ArrayList<>(b.getBindings());

        assertEquals(1, bindings.size());

        Binding binding = bindings.get(0);
        Set types = binding.getContracts();
        assertEquals(expectedTypes.length, types.size());

        Arrays.asList(expectedTypes)
                .forEach(type -> assertTrue(types.contains(type)));
    }
}
