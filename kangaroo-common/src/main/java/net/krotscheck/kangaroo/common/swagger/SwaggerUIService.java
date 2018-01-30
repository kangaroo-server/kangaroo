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

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * The Swagger UI resources assumes that it is the root of the API- though
 * not necessarily the server. It will serve up index.html and any other
 * files contained in the swagger package.
 *
 * @author Michael Krotscheck
 */
@Path("/")
@Singleton
public final class SwaggerUIService {

    /**
     * Files available to be served out of the swagger directory.
     */
    private final List<String> files = Arrays.asList(
            "favicon-16x16.png",
            "favicon-32x32.png",
            "index.html",
            "oauth2-redirect.html",
            "swagger-ui.css",
            "swagger-ui.js",
            "swagger-ui-bundle.js",
            "swagger-ui-standalone-preset.js"
    );

    /**
     * The classloader used to resolve any existing resources from the
     * swagger bundle.
     */
    private final ClassLoader classLoader = getClass().getClassLoader();

    /**
     * If the root resource is requested, return index.html.
     *
     * @return An InputStream containing the swagger index.html
     */
    @GET
    public InputStream getFile() {
        return specificFile("index.html");
    }

    /**
     * Return any static resource found in the swagger directory, assuming it
     * exists.
     *
     * @param path The file path.
     * @return An inputstream for the resource.
     */
    @GET
    @Path("{path: [^./]+\\.(html|css|js|png)}")
    public InputStream specificFile(@PathParam("path") final String path) {
        // Only permit reading those files that we know about.
        if (!files.contains(path)) {
            throw new NotFoundException();
        }
        String resourcePath = String.format("swagger/%s", path);
        return classLoader.getResourceAsStream(resourcePath);
    }
}
