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

package net.krotscheck.api.admin.v1;

import net.krotscheck.test.DatabaseTest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

/**
 * Test for the admin API.
 */
public final class AdminV1APITest extends DatabaseTest {

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
