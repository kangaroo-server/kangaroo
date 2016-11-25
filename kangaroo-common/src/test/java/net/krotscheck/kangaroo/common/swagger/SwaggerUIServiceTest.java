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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the swagger UI host.
 *
 * @author Michael Krotscheck
 */
public final class SwaggerUIServiceTest extends ContainerTest {

    /**
     * Create a blank application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(SwaggerUIService.class);
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
     * Assert that all documents in the swagger directory are available.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void routeAllResources() throws Exception {

        ClassLoader classLoader = SwaggerUIService.class.getClassLoader();
        InputStream dir = classLoader.getResourceAsStream("swagger");
        BufferedReader in = new BufferedReader(new InputStreamReader(dir));
        String line;

        try {
            while ((line = in.readLine()) != null) {
                Response r = target(line).request().get();
                assertEquals(200, r.getStatus());

                r.bufferEntity();
                String body = r.readEntity(String.class);
                String expectedBody = IOUtils.toString(
                        classLoader.getResourceAsStream(
                                String.format("swagger/%s", line)
                        )
                );
                assertEquals(expectedBody, body);
                r.close();
            }
        } finally {
            IOUtils.closeQuietly(dir);
        }
    }

    /**
     * Assert that documents that do not exist fall through to the error
     * handler.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void routeErrorHandler() throws Exception {
        Response r = target("/invalid.html").request().get();
        assertEquals(404, r.getStatus());
    }
}
