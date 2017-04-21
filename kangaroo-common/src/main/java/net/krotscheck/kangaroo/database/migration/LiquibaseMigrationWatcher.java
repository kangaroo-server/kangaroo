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

package net.krotscheck.kangaroo.database.migration;

import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.ChangeSet.ExecType;
import liquibase.changelog.ChangeSet.RunStatus;
import liquibase.changelog.ChangeSetStatus;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.precondition.core.PreconditionContainer.ErrorOption;
import liquibase.precondition.core.PreconditionContainer.FailOption;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A very simple observer of our migration state. It collects statistics
 * that then inform the remaining system.
 *
 * @author Michael Krotscheck
 */
public final class LiquibaseMigrationWatcher implements ChangeExecListener {

    /**
     * The total number of migrations that were run during this iteration.
     */
    private Integer totalRun = 0;

    /**
     * The last recorded changeset.
     */
    private ChangeSet lastVersion;

    /**
     * Create a new migration watcher.
     *
     * @param changesets The list of all expected change sets.
     */
    public LiquibaseMigrationWatcher(final List<ChangeSetStatus> changesets) {
        List<ChangeSet> changeSets = changesets.stream()
                .filter(ChangeSetStatus::getPreviouslyRan)
                .map(ChangeSetStatus::getChangeSet)
                .collect(Collectors.toList());

        if (changeSets.size() > 0) {
            lastVersion = changeSets.get(changeSets.size() - 1);
        }
    }

    /**
     * Called right before a run is started.
     *
     * @param changeSet         The changeset.
     * @param databaseChangeLog The current changelog.
     * @param database          A reference to the database.
     * @param runStatus         The current run status.
     */
    @Override
    public void willRun(final ChangeSet changeSet,
                        final DatabaseChangeLog databaseChangeLog,
                        final Database database,
                        final RunStatus runStatus) {
        // Do nothing.
    }

    /**
     * Executed after a migration was successfully applied.
     *
     * @param changeSet         The changeset.
     * @param databaseChangeLog The current changelog.
     * @param database          A reference to the database.
     * @param execType          The execution type (update, etc).
     */
    @Override
    public void ran(final ChangeSet changeSet,
                    final DatabaseChangeLog databaseChangeLog,
                    final Database database,
                    final ExecType execType) {
        lastVersion = changeSet;
        totalRun++;
    }

    /**
     * Executed after a change was rolled back.
     *
     * @param changeSet         The changeset.
     * @param databaseChangeLog The current changelog.
     * @param database          A reference to the database.
     */
    @Override
    public void rolledBack(final ChangeSet changeSet,
                           final DatabaseChangeLog databaseChangeLog,
                           final Database database) {
        // Do nothing.
    }

    /**
     * An error occurred during the migration.
     *
     * @param error  The error.
     * @param onFail What should we do on failure.
     */
    @Override
    public void preconditionFailed(final PreconditionFailedException error,
                                   final FailOption onFail) {
        // Do nothing.
    }

    /**
     * An error occurred during the migration.
     *
     * @param error   The error.
     * @param onError What should we do on failure.
     */
    @Override
    public void preconditionErrored(final PreconditionErrorException error,
                                    final ErrorOption onError) {
        // Do nothing.
    }

    /**
     * Called right before a run is started.
     *
     * @param change    The change that was applied.
     * @param changeSet The changeset.
     * @param changeLog The current changelog.
     * @param database  A reference to the database.
     */
    @Override
    public void willRun(final Change change,
                        final ChangeSet changeSet,
                        final DatabaseChangeLog changeLog,
                        final Database database) {
        // Do nothing.
    }

    /**
     * The run succeeded.
     *
     * @param change    The change that was applied.
     * @param changeSet The changeset.
     * @param changeLog The current changelog.
     * @param database  A reference to the database.
     */
    @Override
    public void ran(final Change change,
                    final ChangeSet changeSet,
                    final DatabaseChangeLog changeLog,
                    final Database database) {
        // Do nothing.
    }

    /**
     * The run failed, what do we do?
     *
     * @param changeSet         The changeset.
     * @param databaseChangeLog The current changelog.
     * @param database          A reference to the database.
     * @param exception         The exception that occurred.
     */
    @Override
    public void runFailed(final ChangeSet changeSet,
                          final DatabaseChangeLog databaseChangeLog,
                          final Database database,
                          final Exception exception) {
        // Do nothing.
    }

    /**
     * Did a migration happen?
     *
     * @return True if yes, otherwise false.
     */
    public Boolean isMigrated() {
        return totalRun > 0;
    }

    /**
     * Get the current version of the database.
     *
     * @return The current sha reference.
     */
    public ChangeSet getCurrentVersion() {
        return lastVersion;
    }
}
