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

package net.krotscheck.features.database;

import net.krotscheck.features.database.listener.CreatedUpdatedListener;
import net.krotscheck.jersey2.hibernate.HibernateFeature;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Database management feature, including hibernate hooks, filters, and the data
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

        return true;
    }
}
