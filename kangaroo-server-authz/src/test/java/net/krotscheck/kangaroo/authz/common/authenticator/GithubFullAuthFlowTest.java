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

package net.krotscheck.kangaroo.authz.common.authenticator;

import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.IntegrationTest;
import net.krotscheck.kangaroo.test.runner.SingleInstanceTestRunner;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.net.URI;

import static net.krotscheck.kangaroo.test.TestConfig.getGithubAccountId;
import static net.krotscheck.kangaroo.test.TestConfig.getGithubAccountSecret;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.support.ui.ExpectedConditions.and;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

/**
 * This test performs a series of full login flows against github.
 *
 * @author Michael Krotscheck
 */
@Category(IntegrationTest.class)
@RunWith(SingleInstanceTestRunner.class)
public final class GithubFullAuthFlowTest
        extends AbstractBrowserLoginTest {

    /**
     * The selenium screenshot prefix.
     */
    private static final String S_PREFIX = "github";

    /**
     * Reset the github account before every test.
     */
    @Before
    public void githubLogin() {
        WebDriver d = SELENIUM.getDriver();

        // Login to github.
        d.get("https://github.com/login");
        new WebDriverWait(d, TIMEOUT)
                .until(and(
                        elementToBeClickable(By.id("login_field")),
                        elementToBeClickable(By.id("password"))
                ));
        SELENIUM.screenshot(S_PREFIX);

        d.findElement(By.id("login_field")).clear();
        d.findElement(By.id("login_field"))
                .sendKeys(getGithubAccountId());
        d.findElement(By.id("password")).clear();
        d.findElement(By.id("password"))
                .sendKeys(getGithubAccountSecret());
        d.findElement(By.cssSelector("div#login input[type='submit']"))
                .click();
        SELENIUM.screenshot(S_PREFIX);
        new WebDriverWait(d, TIMEOUT)
                .until(presenceOfElementLocated(By.id("user-links")));
        SELENIUM.screenshot(S_PREFIX);
    }

    /**
     * Reset the github account before every test.
     */
    @After
    public void githubLogout() {
        WebDriver d = SELENIUM.getDriver();
        // Capture the last state before an error.
        SELENIUM.screenshot(S_PREFIX);

        d.get("https://github.com/logout");
        SELENIUM.screenshot(S_PREFIX);
        new WebDriverWait(d, TIMEOUT)
                .until(presenceOfElementLocated(
                        By.cssSelector("form input[type='submit']")))
                .click();
        SELENIUM.screenshot(S_PREFIX);
        new WebDriverWait(d, TIMEOUT)
                .until(presenceOfElementLocated(
                        By.id("user[login]")));
    }

    /**
     * A simple, round-trip login via github. This presumes that your
     * user has already logged in and approved the application.
     */
    @Test
    public void testNewLogin() {
        String testState = RandomStringUtils.randomAlphanumeric(20);
        SELENIUM.screenshot(S_PREFIX);

        // Issue a request against our /authorize endpoint.
        URI requestUri = UriBuilder.fromUri(getBaseUri())
                .path("/oauth2/authorize")
                .queryParam("authenticator", AuthenticatorType.Github)
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(getContext().getClient().getId()))
                .queryParam("scope", "debug")
                .queryParam("state", testState)
                .build();

        WebDriver d = SELENIUM.getDriver();
        d.get(requestUri.toString());

        // We should go to the "redirect" at this point.
        (new WebDriverWait(d, TIMEOUT))
                .until(ExpectedConditions.urlContains("www.example.com"));
        SELENIUM.screenshot(S_PREFIX);

        String url = d.getCurrentUrl();
        URI uri = URI.create(url);
        MultivaluedMap<String, String> params = HttpUtil.parseQueryParams(uri);
        BigInteger code = IdUtil.fromString(params.getFirst("code"));
        assertEquals(testState, params.getFirst("state"));

        OAuthToken authToken = getSession().get(OAuthToken.class, code);
        assertEquals(authToken.getClient().getId(),
                getContext().getClient().getId());
        assertEquals(AuthenticatorType.Github,
                authToken.getIdentity().getType());
        assertEquals(OAuthTokenType.Authorization,
                authToken.getTokenType());
    }
}
