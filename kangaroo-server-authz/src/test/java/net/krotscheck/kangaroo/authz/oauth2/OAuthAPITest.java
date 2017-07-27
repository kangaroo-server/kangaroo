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

package net.krotscheck.kangaroo.authz.oauth2;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.test.jerseyTest.ContainerTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Smoke test that the OAuthAPI can be loaded.
 *
 * @author Michael Krotscheck
 */
public final class OAuthAPITest extends ContainerTest {

    /**
     * Create a test instance of the application to test against.
     *
     * @return The constructed application.
     */
    @Override
    protected ResourceConfig createApplication() {
        return new OAuthAPI();
    }

    /**
     * The application smoketest.
     */
    @Test
    public void smokeTest() {
        Response response = target("/")
                .request()
                .get();

        ErrorResponse e = response.readEntity(ErrorResponse.class);
        Assert.assertEquals("not_found", e.getError());
        Assert.assertEquals("HTTP 404 Not Found", e.getErrorDescription());

        // Root should be 404
        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(),
                response.getStatus());
    }
}
