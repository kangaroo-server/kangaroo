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

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for our common service abstractions.
 *
 * @author Michael Krotscheck
 */
public final class AbstractServiceTest {

    /**
     * Ensure the constructor behaves as expected.
     */
    @Test
    public void testConstructor() {
        Session session = Mockito.mock(Session.class);
        SearchFactory factory = Mockito.mock(SearchFactory.class);
        FullTextSession ftSession = Mockito.mock(FullTextSession.class);
        AbstractService s = new TestService(session, factory, ftSession);

        Assert.assertEquals(session, s.getSession());
        Assert.assertEquals(factory, s.getSearchFactory());
        Assert.assertEquals(ftSession, s.getFullTextSession());
    }

    /**
     * Concrete implementation of the abstract service, for testing.
     */
    public static final class TestService extends AbstractService {

        /**
         * Create a new instance of the test service.
         *
         * @param session         The Hibernate session.
         * @param searchFactory   The FT Search factory.
         * @param fullTextSession The fulltext search factory.
         */
        public TestService(final Session session,
                           final SearchFactory searchFactory,
                           final FullTextSession fullTextSession) {
            super(session, searchFactory, fullTextSession);
        }
    }
}
