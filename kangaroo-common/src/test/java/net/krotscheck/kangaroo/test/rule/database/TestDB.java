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

package net.krotscheck.kangaroo.test.rule.database;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDB53Dialect;
import org.hibernate.dialect.MariaDBDialect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Enum of databases supported in the test framework.
 *
 * @author Michael Krotscheck
 */
public enum TestDB {

    /**
     * A MariaDB database (Also usable for mysql).
     */
    MARIADB(MariaDBDialect.class, MariaDB53Dialect.class),

    /**
     * An H2 database.
     */
    H2(H2Dialect.class);

    /**
     * The db's dialect.
     */
    private final Set<Class<? extends Dialect>> dialects = new HashSet<>();

    /**
     * Create a new enum instance.
     *
     * @param setDialects The dialects for this db type.
     */
    TestDB(final Class<? extends Dialect>... setDialects) {
        dialects.addAll(Arrays.asList(setDialects));
    }

    /**
     * Find the correct DB type from a dialect string.
     *
     * @param dbDialect The dialect string.
     * @return The DB Type, or H2 as the default.
     */
    public static TestDB fromDialect(final String dbDialect) {
        try {
            return fromDialect(Class.forName(dbDialect));
        } catch (ClassNotFoundException cnfe) {
            return H2;
        }
    }

    /**
     * Find the correct DB type from a dialect class.
     *
     * @param dialect The dialect class.
     * @return The DB Type, or H2 as the default.
     */
    public static TestDB fromDialect(final Class dialect) {
        for (TestDB value : TestDB.values()) {
            if (value.dialects.contains(dialect)) {
                return value;
            }
        }
        return H2;
    }
}
