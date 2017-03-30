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

package net.krotscheck.kangaroo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * A resource access utility that lets us just read a resource directly to a
 * prerequested format.
 *
 * @author Michael Krotscheck
 */
public final class ResourceUtil {

    /**
     * Private constructor for a utility class.
     */
    private ResourceUtil() {

    }

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory.getLogger(ResourceUtil.class);

    /**
     * Get a file for a resource.
     *
     * @param resourcePath The path to resolve.
     * @return The resource, as a file instance.
     */
    public static File getFileForResource(final String resourcePath) {
        URL path = ResourceUtil.class.getResource(resourcePath);
        if (path == null) {
            path = ResourceUtil.class.getResource("/");
            return new File(new File(path.getPath()), resourcePath);
        }

        return new File(path.getPath());
    }
}
