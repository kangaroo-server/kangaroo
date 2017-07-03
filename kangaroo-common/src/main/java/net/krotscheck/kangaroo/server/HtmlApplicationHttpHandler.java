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

package net.krotscheck.kangaroo.server;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandlerBase;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.io.File;

/**
 * This handler enables our grizzly server to serve HTML applications, which
 * we define as a rich javascript client, able to manage its own
 * routing via the HTML5 history API. For these, the server needs to return
 * the root directory's index.html file whenever a requested resource is not
 * found. In all other cases, it should simply return the requested resource.
 *
 * @author Michael Krotscheck
 */
public final class HtmlApplicationHttpHandler extends StaticHttpHandlerBase {

    /**
     * The document root path.
     */
    private final File docRoot;

    /**
     * The root index.html file.
     */
    private final File indexResource;

    /**
     * Create a new instance which will look for static pages located
     * under the <tt>docRoot</tt>.
     *
     * @param docRoot the folder where the static resource are located.
     */
    public HtmlApplicationHttpHandler(final String docRoot) {
        this.docRoot = new File(docRoot);

        // Make sure that we have an indexResource.html.
        this.indexResource = new File(this.docRoot, "/index.html");
        if (!this.indexResource.exists()) {
            throw new RuntimeException("docRoot does not contain index.html");
        }
    }

    /**
     * Each HTTP Request is checked to see if it exists. If it is not, or if
     * it is a directory, return the index.html file from the root directory.
     * Otherwise, simply return the resource.
     */
    @Override
    protected boolean handle(final String uri,
                             final Request request,
                             final Response response) throws Exception {

        // If it's not HTTP GET - return method is not supported status
        if (!Method.GET.equals(request.getMethod())) {
            // TODO(krotscheck): return common error format.
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            response.setHeader(Header.Allow, "GET");
            return true;
        }

        // local file
        File resource = new File(docRoot, uri);
        final boolean exists = resource.exists();
        final boolean isDirectory = resource.isDirectory();

        // If it doesn't exist, or is a directory, simply return index.html
        if (!exists || isDirectory) {
            resource = indexResource;
        }

        // All responses have a small number of security headers that must be
        // added to all requests.
        SecurityHeaders.ALL.forEach(response::addHeader);

        pickupContentType(response, resource.getPath());

        addToFileCache(request, response, resource);
        sendFile(response, resource);

        return true;
    }
}
