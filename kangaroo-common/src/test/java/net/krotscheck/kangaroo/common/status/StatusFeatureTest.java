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
 *
 */

package net.krotscheck.kangaroo.common.status;

import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import net.krotscheck.kangaroo.test.KangarooJerseyTest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * General smoke test for the status feature.
 *
 * @author Michael Krotscheck
 */
public final class StatusFeatureTest extends KangarooJerseyTest {

    /**
     * Configure the test application.
     *
     * @return A configured app.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(StatusFeature.class);
        return a;
    }

    /**
     * Smoke test a simple status request.
     */
    @Test
    public void testSimpleRequest() {
        Response r = target("/status").request().get();
        Assert.assertEquals(HttpStatus.SC_OK, r.getStatus());
    }
}
