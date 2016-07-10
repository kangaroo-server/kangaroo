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

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.servlet.admin.v1.AdminV1API;
import net.krotscheck.kangaroo.test.ContainerTest;

import org.glassfish.jersey.test.TestProperties;

import javax.ws.rs.core.Application;

/**
 * Abstract test harness for the administration API. Handles all of our data
 * bootstrapping.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractResourceTest extends ContainerTest {

    /**
     * Create the application under test.
     *
     * @return A configured api servlet.
     */
    @Override
    protected final Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new AdminV1API();
    }
}
