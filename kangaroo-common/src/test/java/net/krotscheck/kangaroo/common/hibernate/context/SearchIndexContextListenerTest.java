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

package net.krotscheck.kangaroo.common.hibernate.context;

import net.krotscheck.kangaroo.test.DatabaseTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;


/**
 * Unit test for our lucene indexer.
 *
 * @author Michael Krotscheck
 */
public final class SearchIndexContextListenerTest extends DatabaseTest {

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception An exception that indicates a failed fixture load.
     */
    @Override
    public List<EnvironmentBuilder> fixtures() throws Exception {
        return null;
    }

    /**
     * Assert that the session factory is properly created.
     */
    @Test
    public void createSessionFactory() {
        SearchIndexContextListener listener = new SearchIndexContextListener();
        SessionFactory factory = listener.createSessionFactory();
        // It's not easy to introspect into what's being loaded, but we need
        // this for coverage.
        Assert.assertNotNull(factory);
    }
}
