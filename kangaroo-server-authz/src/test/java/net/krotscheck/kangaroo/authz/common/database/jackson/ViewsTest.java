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

package net.krotscheck.kangaroo.authz.common.database.jackson;

import net.krotscheck.kangaroo.authz.common.database.jackson.Views.Public;
import net.krotscheck.kangaroo.authz.common.database.jackson.Views.Secure;
import org.junit.Test;

/**
 * Smoke test for the jackson view annotation classes.
 *
 * @author Michael Krotscheck
 */
public final class ViewsTest {

    /**
     * Test that all three classes exist.
     */
    @Test
    public void assertAllClasses() {
        Views views = new Views();
        Public pub = new Views.Public();
        Secure secure = new Views.Secure();
    }
}
