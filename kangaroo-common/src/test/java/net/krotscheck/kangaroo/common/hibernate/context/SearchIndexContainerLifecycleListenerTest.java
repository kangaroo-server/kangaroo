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

import net.krotscheck.kangaroo.common.hibernate.lifecycle.SearchIndexContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


/**
 * Unit test for our lucene indexer.
 *
 * @author Michael Krotscheck
 */
@RunWith(PowerMockRunner.class)
@PrepareOnlyThisForTest({Search.class})
public final class SearchIndexContainerLifecycleListenerTest {

    /**
     * The mass indexer, from setup.
     */
    private MassIndexer mockIndexer;

    /**
     * The mock session factory, from setup.
     */
    private SessionFactory mockFactory;

    /**
     * The mock session, from setup.
     */
    private Session mockSession;

    /**
     * Listener under test.
     */
    private SearchIndexContainerLifecycleListener listener;

    /**
     * Mock container, for testing.
     */
    private Container container;

    /**
     * Create a new listener.
     */
    @Before
    public void setupTest() {
        // Set up a fake session factory
        mockFactory = mock(SessionFactory.class);
        mockSession = mock(Session.class);
        when(mockFactory.openSession()).thenReturn(mockSession);

        // Set up a fake indexer
        mockIndexer = mock(MassIndexer.class);

        // Set up our fulltext session.
        FullTextSession mockFtSession = mock(FullTextSession.class);
        when(mockFtSession.createIndexer()).thenReturn(mockIndexer);

        // This is the way to tell PowerMock to mock all static methods of a
        // given class
        mockStatic(Search.class);
        when(Search.getFullTextSession(mockSession))
                .thenReturn(mockFtSession);

        listener = new SearchIndexContainerLifecycleListener(mockFactory);
        container = mock(Container.class);
    }

    /**
     * Assert that the index is created on startup.
     *
     * @throws Exception Any unexpected exceptions.
     */
    @Test
    public void testOnStartup() throws Exception {

        listener.onStartup(container);

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
    public void testInterruptedIndex() throws Exception {
        doThrow(new InterruptedException())
                .when(mockIndexer)
                .startAndWait();

        // Run the test
        listener.onStartup(container);

        // Verify that the session was closed.
        verify(mockSession).close();
    }

    /**
     * Assert that all exceptions are caught.
     *
     * @throws Exception An exception that might be thrown.
     */
    @Test
    public void testRandomException() throws Exception {
        doThrow(new RuntimeException())
                .when(mockIndexer)
                .startAndWait();

        try {
            listener.onStartup(container);
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertNotNull(e);
        }

        // Verify that the session was closed.
        verify(mockSession).close();
    }

    /**
     * Assert that the reload method does not impact the container.
     *
     * @throws Exception An exception that might be thrown.
     */
    @Test
    public void testReload() throws Exception {
        listener.onReload(container);
        verifyZeroInteractions(container);
    }

    /**
     * Assert that the shutdown method does not impact the container.
     *
     * @throws Exception An exception that might be thrown.
     */
    @Test
    public void testShutdown() throws Exception {
        listener.onShutdown(container);
        verifyZeroInteractions(container);
    }
}
