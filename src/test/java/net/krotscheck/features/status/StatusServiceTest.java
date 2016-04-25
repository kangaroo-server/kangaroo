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

package net.krotscheck.features.status;

import net.krotscheck.features.config.ConfigurationFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Application;

/**
 * Unit tests to ensure that the status service returns the expected results.
 *
 * @author Michael Krotscheck
 */
public final class StatusServiceTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig a = new ResourceConfig();
        a.register(ConfigurationFeature.class);
        a.register(StatusService.class);

        return a;
    }

    @Test
    public void testStatus() throws Exception {
        StatusResponse response =
                target("/").request().get(StatusResponse.class);
        Assert.assertEquals("dev", response.getVersion());
    }
}