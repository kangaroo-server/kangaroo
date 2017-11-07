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
import net.krotscheck.kangaroo.common.hibernate.migration.DatabaseMigrationState;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.glassfish.jersey.server.spi.Container;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for our lucene indexer.
 *
 * @author Michael Krotscheck
 */
public final class SearchIndexContainerLifecycleListenerTest
        extends DatabaseTest {

    /**
     * The mass indexer, from setup.
     */
    private MassIndexer mockIndexer;

    /**
     * The mock session factory, from setup.
     */
    private SessionFactory mockFactory;

    /**
     * Simulated migration state.
     */
    private DatabaseMigrationState mockMigrationState;

    /**
     * The test session, from setup.
     */
    private Session testSession;

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
        testSession = spy(getSession());
        when(mockFactory.openSession()).thenReturn(testSession);

        // Set up a fake indexer
        mockIndexer = mock(MassIndexer.class);
        mockMigrationState = mock(DatabaseMigrationState.class);
        doReturn(true).when(mockMigrationState).isSchemaChanged();

        // Set up our fulltext session.
        FullTextSession mockFtSession = mock(FullTextSession.class);
        when(mockFtSession.createIndexer()).thenReturn(mockIndexer);

        listener = spy(new SearchIndexContainerLifecycleListener(mockFactory,
                mockMigrationState));
        container = mock(Container.class);

        doReturn(mockFtSession).when(listener).getFulltextSession(any());
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
        verify(testSession).close();
    }

    /**
     * Assert that the index is created on startup.
     *
     * @throws Exception Any unexpected exceptions.
     */
    @Test
    public void testNoReindex() throws Exception {
        doReturn(false).when(mockMigrationState).isSchemaChanged();

        listener.onStartup(container);

        verifyZeroInteractions(container);
        verifyZeroInteractions(mockIndexer);
        verifyZeroInteractions(mockFactory);
        verifyZeroInteractions(testSession);
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
        verify(testSession).close();
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
            assertNotNull(e);
        }

        // Verify that the session was closed.
        verify(testSession).close();
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

    /**
     * Coverage concession- since we're mocking the getFactory method above,
     * this actually calls it.
     *
     * @throws Exception An exception that might be thrown.
     */
    @Test
    public void testBuildIndexerPassthrough() throws Exception {
        listener = new SearchIndexContainerLifecycleListener(mockFactory,
                mockMigrationState);
        FullTextSession f = listener.getFulltextSession(testSession);
        assertNotNull(f);
    }
}
