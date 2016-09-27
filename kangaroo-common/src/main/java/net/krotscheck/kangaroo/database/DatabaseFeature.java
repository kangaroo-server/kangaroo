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

package net.krotscheck.kangaroo.database;

import net.krotscheck.jersey2.hibernate.HibernateFeature;
import net.krotscheck.kangaroo.database.listener.CreatedUpdatedListener;
import net.krotscheck.kangaroo.database.mapper.ConstraintViolationExceptionMapper;
import net.krotscheck.kangaroo.database.mapper.HibernateExceptionMapper;
import net.krotscheck.kangaroo.database.mapper.PropertyValueExceptionMapper;
import net.krotscheck.kangaroo.database.mapper.QueryExceptionMapper;
import net.krotscheck.kangaroo.database.mapper.SearchExceptionMapper;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Database management feature, including hibernate hooks, filters, and the
 * data
 * model.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseFeature implements Feature {

    /**
     * Register this feature.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Pull in the Jackson provider, but not the exception mappers.
        context.register(HibernateFeature.class);

        // Listeners
        context.register(new CreatedUpdatedListener.Binder());
        context.register(new QueryExceptionMapper.Binder());
        context.register(new HibernateExceptionMapper.Binder());
        context.register(new ConstraintViolationExceptionMapper.Binder());
        context.register(new PropertyValueExceptionMapper.Binder());
        context.register(new SearchExceptionMapper.Binder());

        return true;
    }
}
