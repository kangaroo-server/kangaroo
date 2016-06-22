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

package net.krotscheck.api.oauth.rfc6749;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.api.oauth.OAuthAPI;
import net.krotscheck.api.oauth.OAuthTestApp;
import net.krotscheck.test.ContainerTest;
import org.glassfish.jersey.test.TestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;

/**
 * Abstract testing class that bootstraps a full OAuthAPI that's ready for
 * external hammering.
 *
 * @author Michael Krotscheck
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-2.3">https://tools.ietf.org/html/rfc6749#section-2.3</a>
 */
public abstract class AbstractRFC6749Test extends ContainerTest {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(AbstractRFC6749Test.class);

    /**
     * Jackson object mapper, to assist in decoding things.
     */
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Create a small dummy app to test against.
     *
     * @return The constructed application.
     */
    @Override
    protected final Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new OAuthTestApp();
    }
}
