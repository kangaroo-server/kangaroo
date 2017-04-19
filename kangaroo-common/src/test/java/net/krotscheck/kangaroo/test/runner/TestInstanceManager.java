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

package net.krotscheck.kangaroo.test.runner;

import org.junit.runners.model.TestClass;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Small internal helper pojo, which keeps track of how often this
 * instance was requested.
 *
 * @author Michael Krotscheck
 */
public final class TestInstanceManager {

    /**
     * A static cache of all test classes that have been created.
     */
    private static Map<String, ManagedInstance> instanceCache =
            new HashMap<>();

    /**
     * Return a managed test instance for the specified test class.
     *
     * @param tClass     The test class.
     * @param totalCount The total number of expected invocations.
     * @return A managed test instance.
     * @throws Exception Thrown if the test is malformed.
     */
    public static Object getInstance(final TestClass tClass,
                                     final int totalCount)
            throws Exception {
        return getInstance(tClass, totalCount, (Object[]) null);
    }

    /**
     * Return a managed test instance for the specified test class.
     *
     * @param tClass     The test class.
     * @param totalCount The total number of expected invocations.
     * @param initArgs   Initialization arguments.
     * @return A managed test instance.
     * @throws Exception Thrown if the test is malformed.
     */
    public static Object getInstance(final TestClass tClass,
                                     final int totalCount,
                                     final Object... initArgs)
            throws Exception {

        // Build a unique key for this test class & init args.
        String hashCode = "";
        if (initArgs != null) {
            hashCode = "_" + String.valueOf(Arrays.hashCode(initArgs));
        }
        String key = String.format("%s%s", tClass.getName(), hashCode);

        if (!instanceCache.containsKey(key)) {
            instanceCache.put(key, new ManagedInstance(totalCount,
                    tClass.getOnlyConstructor().newInstance(initArgs)));
        }
        ManagedInstance marker = instanceCache.get(key);
        Object instance = marker.get();
        if (marker.isExhausted()) {
            instanceCache.remove(key);
        }
        return instance;
    }

    /**
     * Utility class, private constructor.
     */
    private TestInstanceManager() {

    }


    /**
     * Small internal helper pojo, which keeps track of how often this
     * instance was requested.
     */
    private static class ManagedInstance {

        /**
         * The total number of test executions we're expecting.
         */
        private final int totalCount;

        /**
         * The total number of times this instance has been requested.
         */
        private int currentCount = 0;

        /**
         * The test instance.
         */
        private final Object instance;

        /**
         * Create a new instance of this marker.
         *
         * @param totalCount The total number of executions we're expecting.
         * @param instance   The instance to manage
         */
        ManagedInstance(final int totalCount,
                        final Object instance) {
            this.totalCount = totalCount;
            this.instance = instance;
        }

        /**
         * Retrieve the instance, but only 'totalCount' times.
         *
         * @return The instance.
         */
        public Object get() {
            currentCount++;
            return instance;
        }

        /**
         * Return true if we've requested the instance totalCount # of times.
         *
         * @return True if we've exhausted this resource, otherwise false.
         */
        public boolean isExhausted() {
            return currentCount >= totalCount;
        }
    }
}
