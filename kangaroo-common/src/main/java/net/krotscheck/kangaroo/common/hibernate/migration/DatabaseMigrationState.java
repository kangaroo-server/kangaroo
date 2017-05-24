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

import liquibase.changelog.ChangeSet;

/**
 * A small, injected POJO that describes the current migration state of the
 * database. Use this during initialization to trigger any actions that need
 * to happen as a result of a schema change.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseMigrationState {

    /**
     * Was the database schema change during this initialization run?
     *
     * @return true if it has changed, otherwise false.
     */
    public boolean isSchemaChanged() {
        return schemaChanged;
    }

    /**
     * The current version / revision of the database schema.
     *
     * @return The current revision.
     */
    public ChangeSet getVersion() {
        return version;
    }

    /**
     * Was the schema changed?
     */
    private final boolean schemaChanged;

    /**
     * What is the version?
     */
    private final ChangeSet version;

    /**
     * Create a new state instance.
     *
     * @param schemaChanged Did the schema change?
     * @param version       The current version of the database.
     */
    public DatabaseMigrationState(final Boolean schemaChanged,
                                  final ChangeSet version) {
        this.schemaChanged = schemaChanged;
        this.version = version;
    }

    /**
     * Convenience constructor.
     */
    public DatabaseMigrationState() {
        this(false, null);
    }
}
