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

package net.krotscheck.kangaroo.servlet.oauth2;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.test.DContainerTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import javax.ws.rs.core.Response;

/**
 * Smoke test that the OAuthAPI can be loaded.
 *
 * @author Michael Krotscheck
 */
public final class OAuthAPITest extends DContainerTest {

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
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<EnvironmentBuilder> fixtures() {
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

        ErrorResponse e = response.readEntity(ErrorResponse.class);
        Assert.assertEquals("not_found", e.getError());
        Assert.assertEquals("Not Found", e.getErrorDescription());

        // Root should be 404
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
}
