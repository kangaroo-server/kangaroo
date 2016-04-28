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

package net.krotscheck.features.database.entity;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the application entity.
 *
 * @author Michael Krotscheck
 */
public final class ApplicationTest {

    /**
     * Test get/set name.
     */
    @Test
    public void testGetSetName() {
        Application a = new Application();

        Assert.assertNull(a.getName());
        a.setName("foo");
        Assert.assertEquals("foo", a.getName());
    }

    /**
     * Test get/set user record.
     */
    @Test
    public void testGetSetApplications() {
        Application a = new Application();
        User u = new User();

        Assert.assertNull(a.getUser());
        a.setUser(u);
        Assert.assertEquals(u, a.getUser());
    }
}
