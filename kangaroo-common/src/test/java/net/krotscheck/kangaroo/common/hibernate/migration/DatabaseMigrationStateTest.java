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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for the DB Migration State injectable.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseMigrationStateTest {

    /**
     * Test that SchemaChanged returns as expected.
     */
    @Test
    public void isSchemaChanged() {
        ChangeSet change = Mockito.mock(ChangeSet.class);

        // Default is false.
        DatabaseMigrationState state1 = new DatabaseMigrationState();
        Assert.assertFalse(state1.isSchemaChanged());

        // Pass true should be true.
        DatabaseMigrationState state2 =
                new DatabaseMigrationState(true, change);
        Assert.assertTrue(state2.isSchemaChanged());

        // Pass false should be false.
        DatabaseMigrationState state3 =
                new DatabaseMigrationState(false, change);
        Assert.assertFalse(state3.isSchemaChanged());
    }

    /**
     * Test the current version of the changeset.
     */
    @Test
    public void getVersion() {
        ChangeSet change = Mockito.mock(ChangeSet.class);

        // Default is null.
        DatabaseMigrationState state1 = new DatabaseMigrationState();
        Assert.assertNull(state1.getVersion());

        // Value should be passed back
        DatabaseMigrationState state2 =
                new DatabaseMigrationState(true, change);
        Assert.assertSame(state2.getVersion(), change);
    }
}
