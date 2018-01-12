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

package net.krotscheck.kangaroo.common.swagger;

import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the swagger UI host.
 *
 * @author Michael Krotscheck
 */
public final class SwaggerFeatureTest extends ContainerTest {

    /**
     * Create a blank application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(new SwaggerFeature("net.krotscheck.kangaroo"));
        return a;
    }

    /**
     * Assert that the index page is accessible at the root resource.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void routeRootResource() throws Exception {
        Response r = target("/").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("text", r.getMediaType().getType());
        assertEquals("html", r.getMediaType().getSubtype());

        r.bufferEntity();
        String body = r.readEntity(String.class);
        String expectedBody = IOUtils.toString(
                getClass().getClassLoader()
                        .getResourceAsStream("swagger/index.html")
        );

        assertEquals(expectedBody, body);
    }

    /**
     * Assert that swagger.json is produced.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void routeSwaggerJsonResource() throws Exception {
        Response r = target("/swagger.json").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("application", r.getMediaType().getType());
        assertEquals("json", r.getMediaType().getSubtype());
    }

    /**
     * Assert that swagger.yml is produced.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void routeSwaggerYamlResource() throws Exception {
        Response r = target("/swagger.yaml").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("application", r.getMediaType().getType());
        assertEquals("yaml", r.getMediaType().getSubtype());
    }
}
