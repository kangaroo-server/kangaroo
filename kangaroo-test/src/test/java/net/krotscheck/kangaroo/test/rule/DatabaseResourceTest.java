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
 */

package net.krotscheck.kangaroo.test.rule;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;

/**
 * Unit tests for the database external resource.
 *
 * @author Michael Krotscheck
 */
public final class DatabaseResourceTest {

    /**
     * Walk through the whole lifecycle. This isn't really intended to test
     * anything, as we don't really ship this (it's only used in tests).
     *
     * @throws Throwable Should not be thrown.
     */
    @Test
    public void testManagerLifecycle() throws Throwable {
        Description desc = Mockito.mock(Description.class);
        DatabaseResource m = new DatabaseResource();

        Statement empty = new Statement() {

            @Override
            public void evaluate() throws Throwable {

            }
        };
        Statement bootstrap = m.apply(empty, desc);
        bootstrap.evaluate();
    }
}
