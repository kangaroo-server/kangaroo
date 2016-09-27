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

package net.krotscheck.kangaroo;

import net.krotscheck.jersey2.hibernate.HibernateFeature;
import net.krotscheck.kangaroo.status.StatusResponse;
import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple smoke test for the API.
 */
public final class RootAPITest extends ContainerTest {

    /**
     * Create a test instance of the application to test against.
     *
     * @return The constructed application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig root = new RootAPI();
        root.register(HibernateFeature.class);
        return root;
    }

    /**
     * The application smoketest.
     */
    @Test
    public void smokeTest() {
        StatusResponse statusResponse = target("/")
                .request()
                .get(StatusResponse.class);
        Assert.assertEquals("dev", statusResponse.getVersion());
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<EnvironmentBuilder> fixtures() {
        return new ArrayList<>();
    }
}
