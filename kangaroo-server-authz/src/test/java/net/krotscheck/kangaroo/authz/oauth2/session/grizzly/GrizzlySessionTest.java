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

package net.krotscheck.kangaroo.authz.oauth2.session.grizzly;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the grizzly session pojo.
 *
 * @author Michael Krotscheck
 */
public final class GrizzlySessionTest {

    /**
     * Assert that we can modify the ID directly.
     */
    @Test
    public void getSetIdInternal() {
        GrizzlySession s = new GrizzlySession();

        assertNull(s.getIdInternal());
        s.setIdInternal("this_is_a_string");
        assertEquals("this_is_a_string", s.getIdInternal());
    }

    /**
     * Assert that we can modify the valid value.
     */
    @Test
    public void isValid() {
        GrizzlySession s = new GrizzlySession();

        assertTrue(s.getTimestamp() > -1);
        assertTrue(s.isValid());
        s.setValid(false);
        assertEquals(-1, s.getTimestamp());
        assertFalse(s.isValid());

        s.setTimestamp(10000);
        s.setValid(true);
        assertTrue(s.isValid());
        assertEquals(10000, s.getTimestamp());
    }

    /**
     * Assert that isNew returns the correct method.
     */
    @Test
    public void isNew() {
        GrizzlySession s = new GrizzlySession();
        assertTrue(s.isNew());
    }

    /**
     * Assert that we can modify the creation time.
     */
    @Test
    public void getSetCreationTime() {
        GrizzlySession s = new GrizzlySession();

        assertTrue(s.getCreationTime() > 0);
        s.setCreationTime(10000);
        assertEquals(10000, s.getCreationTime());
    }

    /**
     * Assert that we can modify the timestamp.
     */
    @Test
    public void getTimestamp() {
        GrizzlySession s = new GrizzlySession();

        assertTrue(s.getTimestamp() > 0);
        s.setTimestamp(10000);
        assertEquals(10000, s.getTimestamp());
    }

    /**
     * Assert that access behaves the same as the parent instance.
     */
    @Test
    public void access() {
        GrizzlySession s = new GrizzlySession();
        assertTrue(s.isNew());
        assertEquals(System.currentTimeMillis(), s.access());
        assertFalse(s.isNew());
    }
}
