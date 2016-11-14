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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletContextEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


/**
 * Unit test for our lucene indexer.
 *
 * @author Michael Krotscheck
 */
@RunWith(PowerMockRunner.class)
public final class SearchIndexContextListenerITest {

    /**
     * Assert that the index is created on startup.
     *
     * @throws Exception Any unexpected exceptions.
     */
    @Test
    @PrepareForTest({Search.class, SearchIndexContextListener.class})
    public void testOnStartup() throws Exception {
        // Set up a fake session factory
        SessionFactory mockFactory = mock(SessionFactory.class);
        Session mockSession = mock(Session.class);
        when(mockFactory.openSession()).thenReturn(mockSession);

        // Set up a fake indexer
        MassIndexer mockIndexer = mock(MassIndexer.class);

        // Set up our fulltext session.
        FullTextSession mockFtSession = mock(FullTextSession.class);
        when(mockFtSession.createIndexer())
                .thenReturn(mockIndexer);

        // This is the way to tell PowerMock to mock all static methods of a
        // given class
        mockStatic(Search.class);
        when(Search.getFullTextSession(mockSession))
                .thenReturn(mockFtSession);

        SearchIndexContextListener listener =
                mock(SearchIndexContextListener.class);

        doReturn(mockFactory).when(listener).createSessionFactory();
        doCallRealMethod().when(listener).contextInitialized(any());

        // Run the test
        listener.contextInitialized(mock(ServletContextEvent.class));

        verify(mockIndexer).startAndWait();

        // Verify that the session was closed.
        verify(mockSession).close();
    }

    /**
     * Assert that an interrupted exception exits cleanly.
     *
     * @throws Exception An exception that might be thrown.
     */
    @Test
    @PrepareForTest({Search.class, SearchIndexContextListener.class})
    public void testInterruptedIndex() throws Exception {
        // Set up a fake session factory
        SessionFactory mockFactory = mock(SessionFactory.class);
        Session mockSession = mock(Session.class);
        when(mockFactory.openSession()).thenReturn(mockSession);

        // Set up a fake indexer
        MassIndexer mockIndexer = mock(MassIndexer.class);

        // Set up our fulltext session.
        FullTextSession mockFtSession = mock(FullTextSession.class);
        when(mockFtSession.createIndexer())
                .thenReturn(mockIndexer);
        doThrow(new InterruptedException())
                .when(mockIndexer)
                .startAndWait();

        // This is the way to tell PowerMock to mock all static methods of a
        // given class
        mockStatic(Search.class);
        when(Search.getFullTextSession(mockSession))
                .thenReturn(mockFtSession);

        SearchIndexContextListener listener =
                mock(SearchIndexContextListener.class);
        doReturn(mockFactory).when(listener).createSessionFactory();
        doCallRealMethod().when(listener).contextInitialized(any());

        // Run the test
        listener.contextInitialized(mock(ServletContextEvent.class));

        // Verify that the session was closed.
        verify(mockSession).close();
        verify(mockFactory).close();
    }

    /**
     * Assert that the disposal method does nothing.
     *
     * @throws Exception An exception that might be thrown.
     */
    @Test
    public void testDisposal() throws Exception {
        ServletContextEvent e = mock(ServletContextEvent.class);
        SearchIndexContextListener listener = new SearchIndexContextListener();
        listener.contextDestroyed(e);

        verifyZeroInteractions(e);
    }
}
