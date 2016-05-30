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

package net.krotscheck.api.oauth.resource;

import net.krotscheck.features.config.ConfigurationFeature;
import net.krotscheck.features.database.DatabaseFeature;
import net.krotscheck.features.exception.ExceptionFeature;
import net.krotscheck.features.jackson.JacksonFeature;
import net.krotscheck.test.ContainerTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

/**
 * Unit tests for the authorization endpoint. Note that this is not a
 * comprehensive suite, as it only covers edge cases and functionality not
 * otherwise covered in our RFC test suites.
 *
 * @author Michael Krotscheck
 */
public final class AuthorizationServiceTest extends ContainerTest {

    /**
     * Build and configure the application.
     *
     * @return A configured application.
     */
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        ResourceConfig a = new ResourceConfig();
        a.register(ConfigurationFeature.class);
        a.register(ExceptionFeature.class);
        a.register(JacksonFeature.class);
        a.register(DatabaseFeature.class);
        a.register(AuthorizationService.class);

        return a;
    }

    /**
     * Smoke test. Does this endpoint exist?
     */
    @Test
    public void testSmoke() {
        Response response = target("/authorize")
                .request()
                .get();

        Assert.assertNotEquals(404, response.getStatus());
    }
}
