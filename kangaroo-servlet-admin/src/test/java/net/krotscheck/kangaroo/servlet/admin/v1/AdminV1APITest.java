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

package net.krotscheck.kangaroo.servlet.admin.v1;

import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.IFixture;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

/**
 * Test for the admin API.
 */
public final class AdminV1APITest extends ContainerTest {

    /**
     * Create a test instance of the application to test against.
     *
     * @return The constructed application.
     */
    @Override
    protected Application configure() {
        return new AdminV1API();
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        return null;
    }

    /**
     * The application smoketest.
     */
    @Test
    public void smokeTest() {
        Response response = target("/")
                .request()
                .get();
        Assert.assertEquals(404, response.getStatus());
    }
}
