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

package net.krotscheck.kangaroo.test;

import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Application;

/**
 * Test the container test, by implementing the container test.
 *
 * @author Michael Krotscheck
 */
public final class ContainerTestFixtureTest extends ContainerTest {

    /**
     * A blank dummy app.
     *
     * @return A dummy app!
     */
    @Override
    protected Application configure() {
        return new ResourceConfig();
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        return new ArrayList<>();
    }

    /**
     * Test that convenience methods are accessible.
     */
    @Test
    public void testTest() {
        Session s = getSession();
        SessionFactory f = getSessionFactory();

        Assert.assertTrue(s.isOpen());
        Assert.assertFalse(f.isClosed());
    }
}
