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
 *
 */

package net.krotscheck.kangaroo.common.hibernate;

import net.krotscheck.kangaroo.common.hibernate.factory.FulltextSearchFactoryFactory;
import net.krotscheck.kangaroo.common.hibernate.factory.FulltextSessionFactory;
import net.krotscheck.kangaroo.common.hibernate.factory.HibernateServiceRegistryFactory;
import net.krotscheck.kangaroo.common.hibernate.factory.HibernateSessionFactory;
import net.krotscheck.kangaroo.common.hibernate.factory.HibernateSessionFactoryFactory;
import net.krotscheck.kangaroo.common.hibernate.factory.PooledDataSourceFactory;
import net.krotscheck.kangaroo.common.hibernate.id.Base16BigIntegerConverterProvider;
import net.krotscheck.kangaroo.common.hibernate.lifecycle.SearchIndexContainerLifecycleListener;
import net.krotscheck.kangaroo.common.hibernate.listener.CreatedUpdatedListener;
import net.krotscheck.kangaroo.common.hibernate.mapper.ConstraintViolationExceptionMapper;
import net.krotscheck.kangaroo.common.hibernate.mapper.HibernateExceptionMapper;
import net.krotscheck.kangaroo.common.hibernate.mapper.PersistenceExceptionMapper;
import net.krotscheck.kangaroo.common.hibernate.mapper.PropertyValueExceptionMapper;
import net.krotscheck.kangaroo.common.hibernate.mapper.QueryExceptionMapper;
import net.krotscheck.kangaroo.common.hibernate.mapper.SearchExceptionMapper;
import net.krotscheck.kangaroo.common.hibernate.migration.LiquibaseMigration;
import net.krotscheck.kangaroo.common.hibernate.transaction.TransactionFilter;
import org.junit.Test;

import javax.ws.rs.core.FeatureContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test that all expected classes are in the hibernate feature.
 *
 * @author Michael Krotscheck
 */
public final class HibernateFeatureTest {


    /**
     * Run a service request.
     */
    @Test
    public void testInjections() {

        HibernateFeature f = new HibernateFeature();
        FeatureContext context = mock(FeatureContext.class);
        f.configure(context);

        verify(context, times(1))
                .register(any(SearchIndexContainerLifecycleListener
                        .Binder.class));
        verify(context, times(1))
                .register(any(CreatedUpdatedListener
                        .Binder.class));
        verify(context, times(1))
                .register(any(LiquibaseMigration
                        .Binder.class));
        verify(context, times(1))
                .register(any(HibernateSessionFactory
                        .Binder.class));
        verify(context, times(1))
                .register(any(HibernateSessionFactoryFactory
                        .Binder.class));
        verify(context, times(1))
                .register(any(HibernateServiceRegistryFactory
                        .Binder.class));
        verify(context, times(1))
                .register(any(FulltextSearchFactoryFactory
                        .Binder.class));
        verify(context, times(1))
                .register(any(FulltextSessionFactory
                        .Binder.class));
        verify(context, times(1))
                .register(any(PooledDataSourceFactory
                        .Binder.class));

        verify(context, times(1))
                .register(any(QueryExceptionMapper
                        .Binder.class));
        verify(context, times(1))
                .register(any(HibernateExceptionMapper
                        .Binder.class));
        verify(context, times(1))
                .register(any(ConstraintViolationExceptionMapper
                        .Binder.class));
        verify(context, times(1))
                .register(any(PersistenceExceptionMapper
                        .Binder.class));
        verify(context, times(1))
                .register(any(PropertyValueExceptionMapper
                        .Binder.class));
        verify(context, times(1))
                .register(any(SearchExceptionMapper
                        .Binder.class));

        verify(context, times(1))
                .register(any(TransactionFilter
                        .Binder.class));

        verify(context, times(1))
                .register(Base16BigIntegerConverterProvider.class);
    }
}
