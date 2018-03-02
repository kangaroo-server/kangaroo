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

package net.krotscheck.kangaroo.test.jersey;

import com.google.common.base.Strings;
import net.krotscheck.kangaroo.common.status.StatusFeature;
import net.krotscheck.kangaroo.test.IntegrationTest;
import net.krotscheck.kangaroo.test.rule.SeleniumRule;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.junit.Assert.assertTrue;


/**
 * This test suite sets up selenium webdriver and local browser instance for
 * direct browser manipulation.
 *
 * @author Michael Krotscheck
 */
@Category(IntegrationTest.class)
public final class SimpleSeleniumTest extends KangarooJerseyTest {

    /**
     * The selenium rule.
     */
    @ClassRule
    public static final SeleniumRule SELENIUM = new SeleniumRule();

    /**
     * Configure the test application.
     *
     * @return A configured app.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();
        a.register(StatusFeature.class);
        return a;
    }

    /**
     * Smoke test a simple status request.
     */
    @Test
    public void testSimpleRequest() {
        // Build the initial URI
        URI statusUri = UriBuilder.fromUri(getBaseUri())
                .path("/status")
                .build();

        WebDriver d = SELENIUM.getDriver();
        d.get(statusUri.toString());
        assertTrue(Strings.isNullOrEmpty(d.getTitle()));
    }
}
