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

package net.krotscheck.kangaroo.common.hibernate.factory;

import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.mchange.v2.c3p0.PooledDataSource;
import net.krotscheck.kangaroo.common.hibernate.factory.PooledDataSourceFactory.Binder;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.glassfish.jersey.internal.inject.Binding;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the pooled data source factory.
 *
 * @author Michael Krotscheck
 */
public final class PooledDataSourceFactoryTest extends DatabaseTest {

    /**
     * Test basic provide from our database.
     */
    @Test
    public void testProvide() {
        PooledDataSourceFactory factory =
                new PooledDataSourceFactory(getSessionFactory());

        PooledDataSource ds = factory.get();
        assertNotNull(ds);
        assertTrue(ds instanceof PoolBackedDataSource);
    }

    /**
     * Assert that we can inject values using this binder.
     */
    @Test
    public void testBinder() {
        Binder b = new PooledDataSourceFactory.Binder();
        List<Binding> bindings = new ArrayList<>(b.getBindings());

        assertEquals(1, bindings.size());

        Binding binding = bindings.get(0);
        Set types = binding.getContracts();
        assertTrue(types.contains(PooledDataSource.class));
    }
}
