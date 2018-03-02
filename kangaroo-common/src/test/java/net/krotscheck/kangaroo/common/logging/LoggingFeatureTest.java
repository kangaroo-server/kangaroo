/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.common.logging;

import ch.qos.logback.classic.Level;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.test.LoggingRule;
import net.krotscheck.kangaroo.test.jersey.KangarooJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Feature loading test for the logging feature.
 *
 * @author Michael krotscheck
 */
public class LoggingFeatureTest extends KangarooJerseyTest {

    /**
     * The logger rule.
     */
    @Rule
    public final LoggingRule logs =
            new LoggingRule(HttpResponseLoggingFilter.class, Level.ALL);

    /**
     * Clean the current logs.
     */
    @Before
    public void clearLogs() {
        // Clear the logs
        logs.clear();
    }

    /**
     * Build an application.
     *
     * @return A configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(JacksonFeature.class);
        config.register(LoggingFeature.class);
        config.register(MockService.class);

        return config;
    }

    /**
     * It should load the logging filter.
     */
    @Test
    public void test200ErrorCodes() {
        target("/error/200")
                .request()
                .get();
        List<String> messages = logs.getMessages();
        assertEquals(1, messages.size());
        assertEquals("200 HTTP GET error/200",
                messages.get(0));
    }

    /**
     * Test entity, returning various different error codes. Test for logging.
     *
     * @author Michael Krotscheck
     */
    @Path("/error")
    public static final class MockService {

        /**
         * Return a 2xx response.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Path("/200")
        @Produces(MediaType.APPLICATION_JSON)
        public Response return200() {
            return Response.ok().build();
        }
    }
}
