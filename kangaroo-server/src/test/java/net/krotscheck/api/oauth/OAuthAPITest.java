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

package net.krotscheck.api.oauth;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.IFixture;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

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
    protected Application configure() {
        return new OAuthAPI();
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
     * Load the test data.
     *
     * @return The test data.
     */
    @Override
    public File testData() {
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
