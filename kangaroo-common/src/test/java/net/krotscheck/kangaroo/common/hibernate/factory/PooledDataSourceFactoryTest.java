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
import net.krotscheck.kangaroo.test.DatabaseTest;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

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

        PooledDataSource ds = factory.provide();
        Assert.assertNotNull(ds);
        Assert.assertTrue(ds instanceof PoolBackedDataSource);
    }

    /**
     * Assert that disposal does nothing (is left to C3P0)
     */
    @Test
    public void testDispose() {
        PooledDataSource mockDS = Mockito.mock(PooledDataSource.class);
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        PooledDataSourceFactory factory =
                new PooledDataSourceFactory(mockFactory);
        factory.dispose(mockDS);
        Mockito.verifyNoMoreInteractions(mockDS);
    }


}