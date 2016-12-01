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

package net.krotscheck.kangaroo.test.rule.hibernate;

import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.RAMDirectoryProvider;
import org.hibernate.search.store.spi.DirectoryHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This component is a hibernate search directory provider intended for use
 * during Jersey2 tests, where the lifecycle of the search index needs to be
 * decoupled from the application container and independently managed.
 * <p>
 * Under the hood it is simply a RamDirectoryProvider, however one that must
 * be explicitly (statically) initialized and cleared. This leads to a far
 * more performant test environment, as filesystem and/or infinispan based
 * directories can slow down the testing harness.
 *
 * @author Michael Krotscheck
 */
public final class TestDirectoryProvider
        implements DirectoryProvider<RAMDirectory> {

    /**
     * Static, shared storage of all providers.
     */
    private static Map<String, RAMDirectory> cachedProviders = new HashMap<>();

    /**
     * The number of times this particular cache has been requested.
     */
    private static Map<String, Integer> referenceCounts = new HashMap<>();

    /**
     * The name of the index.
     */
    private String indexName;

    @Override
    public void initialize(final String directoryProviderName,
                           final Properties properties,
                           final BuildContext context) {
        this.indexName = directoryProviderName;
    }


    @Override
    public void start(final DirectoryBasedIndexManager indexManager) {
        if (cachedProviders.containsKey(indexName)) {
            return;
        }

        RAMDirectory d = new RAMDirectory(NoLockFactory.INSTANCE);
        cachedProviders.put(indexName, d);
        DirectoryHelper.initializeIndexIfNeeded(d);
        referenceCounts.put(indexName, 0);
    }

    @Override
    public synchronized RAMDirectory getDirectory() {
        Integer referenceCount = referenceCounts.get(indexName);
        referenceCounts.put(indexName, referenceCount + 1);
        return cachedProviders.get(indexName);
    }

    @Override
    public synchronized void stop() {
        Integer referenceCount = referenceCounts.get(indexName);
        referenceCounts.put(indexName, referenceCount - 1);

        if (referenceCounts.get(indexName) == 0) {
            RAMDirectory d = cachedProviders.get(indexName);
            d.close();
            cachedProviders.remove(indexName);
        }
    }

    /**
     * Equality operator.
     *
     * @param obj The object to test.
     * @return Whether these two objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        // this code is actually broken since the value change after initialize call
        // but from a practical POV this is fine since we only call this method
        // after initialize call
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof RAMDirectoryProvider)) {
            return false;
        }
        return indexName.equals(((TestDirectoryProvider) obj).indexName);
    }

    /**
     * Hashcode generation off the index name.
     *
     * @return The hashcode.
     */
    @Override
    public int hashCode() {
        // this code is actually broken since the value change after initialize call
        // but from a practical POV this is fine since we only call this method
        // after initialize call
        int hash = 7;
        return 29 * hash + indexName.hashCode();
    }
}
