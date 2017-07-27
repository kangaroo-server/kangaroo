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

package net.krotscheck.kangaroo.common.timedtasks;

import net.krotscheck.kangaroo.common.status.StatusFeature;
import net.krotscheck.kangaroo.test.jerseyTest.KangarooJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * General smoke test for the timed task feature.
 *
 * @author Michael Krotscheck
 */
public final class TimedTaskFeatureTest extends KangarooJerseyTest {

    /**
     * Configure the test application.
     *
     * @return A configured app.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(TimedTasksFeature.class);
        a.register(StatusFeature.class);
        return a;
    }

    /**
     * Smoke test a simple status request.
     */
    @Test
    public void testSimpleRequest() {
        Response r = target("/status").request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }
}
