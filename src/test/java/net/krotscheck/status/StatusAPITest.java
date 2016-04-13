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

package net.krotscheck.status;

import net.krotscheck.status.features.status.StatusResponse;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Application;

/**
 * Sanity test that the status API loads as appropriate.
 *
 * @author Michael Krotscheck
 */
public class StatusAPITest extends JerseyTest {

    @Override
    protected Application configure() {
        return new StatusAPI();
    }

    /**
     * Sanity check that yes, we do have an application.
     */
    @Test
    public void testApplication() {
        StatusResponse response = target("/")
                .request()
                .get(StatusResponse.class);
        Assert.assertEquals("dev", response.getVersion());
    }
}