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

package net.krotscheck.kangaroo.common.hibernate.migration;

import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.ChangeSet.ExecType;
import liquibase.changelog.ChangeSet.RunStatus;
import liquibase.changelog.ChangeSetStatus;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.precondition.core.PreconditionContainer.ErrorOption;
import liquibase.precondition.core.PreconditionContainer.FailOption;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the liquibase migration watcher.
 *
 * @author Michael Krotscheck
 */
public final class LiquibaseMigrationWatcherTest {

    /**
     * Test that the constructor works with no changesets.
     */
    @Test
    public void testConstructorWithNoChangesets() {
        List<ChangeSetStatus> changeSets = new ArrayList<>();
        LiquibaseMigrationWatcher w = new LiquibaseMigrationWatcher(changeSets);
        assertNull(w.getCurrentVersion());
    }

    /**
     * Test that the constructor works with a previously run changeset.
     */
    @Test
    public void testConstructorWithRunChangesets() {
        ChangeSet mockSet = Mockito.mock(ChangeSet.class);
        ChangeSetStatus mockStatus = Mockito.mock(ChangeSetStatus.class);
        Mockito.doReturn(true).when(mockStatus).getPreviouslyRan();
        Mockito.doReturn(mockSet).when(mockStatus).getChangeSet();

        List<ChangeSetStatus> changeSets = new ArrayList<>();
        changeSets.add(mockStatus);

        LiquibaseMigrationWatcher w = new LiquibaseMigrationWatcher(changeSets);
        assertSame(mockSet, w.getCurrentVersion());
    }

    /**
     * Test that the constructor works with a non run changeset.
     */
    @Test
    public void testConstructorWithUnrunChangesets() {
        ChangeSet mockSet = Mockito.mock(ChangeSet.class);
        ChangeSetStatus mockStatus = Mockito.mock(ChangeSetStatus.class);
        Mockito.doReturn(false).when(mockStatus).getPreviouslyRan();
        Mockito.doReturn(mockSet).when(mockStatus).getChangeSet();

        List<ChangeSetStatus> changeSets = new ArrayList<>();
        changeSets.add(mockStatus);

        LiquibaseMigrationWatcher w = new LiquibaseMigrationWatcher(changeSets);
        assertNull(w.getCurrentVersion());
    }

    /**
     * Test that the constructor stores the last-run changeset.
     */
    @Test
    public void testConstructorReturnsLastRunChangSet() {
        List<ChangeSetStatus> changeSets = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            ChangeSet mockSet = Mockito.mock(ChangeSet.class);
            ChangeSetStatus mockStatus = Mockito.mock(ChangeSetStatus.class);
            Mockito.doReturn(true).when(mockStatus).getPreviouslyRan();
            Mockito.doReturn(mockSet).when(mockStatus).getChangeSet();
            changeSets.add(mockStatus);
        }
        ChangeSet lastSet = changeSets.get(changeSets.size() - 1)
                .getChangeSet();

        for (int i = 0; i < 10; i++) {
            ChangeSet mockSet = Mockito.mock(ChangeSet.class);
            ChangeSetStatus mockStatus = Mockito.mock(ChangeSetStatus.class);
            Mockito.doReturn(false).when(mockStatus).getPreviouslyRan();
            Mockito.doReturn(mockSet).when(mockStatus).getChangeSet();
            changeSets.add(mockStatus);
        }

        LiquibaseMigrationWatcher w = new LiquibaseMigrationWatcher(changeSets);
        assertSame(lastSet, w.getCurrentVersion());
    }

    /**
     * Test that various implemented methods have no real impact.
     */
    @Test
    public void testUnimplementedMethods() {
        List<ChangeSetStatus> changeSets = new ArrayList<>();
        LiquibaseMigrationWatcher w = new LiquibaseMigrationWatcher(changeSets);
        Change mockChange = Mockito.mock(Change.class);
        ChangeSet mockChangeSet = Mockito.mock(ChangeSet.class);
        DatabaseChangeLog mockDbChangeLog =
                Mockito.mock(DatabaseChangeLog.class);
        Database mockDatabase = Mockito.mock(Database.class);
        PreconditionFailedException mockPFE =
                Mockito.mock(PreconditionFailedException.class);
        PreconditionErrorException mockPEE =
                Mockito.mock(PreconditionErrorException.class);

        w.willRun(mockChangeSet, mockDbChangeLog, mockDatabase,
                RunStatus.MARK_RAN);
        Mockito.verifyNoMoreInteractions(mockChangeSet);
        Mockito.verifyNoMoreInteractions(mockDbChangeLog);
        Mockito.verifyNoMoreInteractions(mockDatabase);

        w.willRun(mockChange, mockChangeSet, mockDbChangeLog, mockDatabase);
        Mockito.verifyNoMoreInteractions(mockChange);
        Mockito.verifyNoMoreInteractions(mockChangeSet);
        Mockito.verifyNoMoreInteractions(mockDbChangeLog);
        Mockito.verifyNoMoreInteractions(mockDatabase);

        w.ran(mockChangeSet, mockDbChangeLog, mockDatabase, ExecType.EXECUTED);
        Mockito.verifyNoMoreInteractions(mockChangeSet);
        Mockito.verifyNoMoreInteractions(mockDbChangeLog);
        Mockito.verifyNoMoreInteractions(mockDatabase);

        w.ran(mockChange, mockChangeSet, mockDbChangeLog, mockDatabase);
        Mockito.verifyNoMoreInteractions(mockChange);
        Mockito.verifyNoMoreInteractions(mockChangeSet);
        Mockito.verifyNoMoreInteractions(mockDbChangeLog);
        Mockito.verifyNoMoreInteractions(mockDatabase);

        w.rolledBack(mockChangeSet, mockDbChangeLog, mockDatabase);
        Mockito.verifyNoMoreInteractions(mockChangeSet);
        Mockito.verifyNoMoreInteractions(mockDbChangeLog);
        Mockito.verifyNoMoreInteractions(mockDatabase);

        w.runFailed(mockChangeSet, mockDbChangeLog, mockDatabase, mockPEE);
        Mockito.verifyNoMoreInteractions(mockChangeSet);
        Mockito.verifyNoMoreInteractions(mockDbChangeLog);
        Mockito.verifyNoMoreInteractions(mockDatabase);
        Mockito.verifyNoMoreInteractions(mockPEE);

        w.preconditionFailed(mockPFE, FailOption.HALT);
        Mockito.verifyNoMoreInteractions(mockPFE);

        w.preconditionErrored(mockPEE, ErrorOption.HALT);
        Mockito.verifyNoMoreInteractions(mockPEE);
    }

    /**
     * Assert that a successfully run migration is recorded.
     */
    @Test
    public void ran() {
        List<ChangeSetStatus> changeSets = new ArrayList<>();
        ChangeSet mockChangeSet = Mockito.mock(ChangeSet.class);
        DatabaseChangeLog mockDbChangeLog =
                Mockito.mock(DatabaseChangeLog.class);
        Database mockDatabase = Mockito.mock(Database.class);

        LiquibaseMigrationWatcher w = new LiquibaseMigrationWatcher(changeSets);

        assertFalse(w.isMigrated());
        assertNull(w.getCurrentVersion());

        w.ran(mockChangeSet, mockDbChangeLog, mockDatabase, ExecType.EXECUTED);

        assertTrue(w.isMigrated());
        assertSame(mockChangeSet, w.getCurrentVersion());
    }
}
