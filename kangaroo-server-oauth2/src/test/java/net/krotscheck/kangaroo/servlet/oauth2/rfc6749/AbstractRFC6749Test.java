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

package net.krotscheck.kangaroo.servlet.oauth2.rfc6749;

import net.krotscheck.kangaroo.servlet.oauth2.OAuthTestApp;
import net.krotscheck.kangaroo.test.ContainerTest;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Abstract testing class that bootstraps a full OAuthAPI that's ready for
 * external hammering.
 *
 * @author Michael Krotscheck
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-2.3">https://tools.ietf.org/html/rfc6749#section-2.3</a>
 */
public abstract class AbstractRFC6749Test extends ContainerTest {

    /**
     * Create a small dummy app to test against.
     *
     * @return The constructed application.
     */
    @Override
    protected final ResourceConfig createApplication() {
        return new OAuthTestApp();
    }
}
