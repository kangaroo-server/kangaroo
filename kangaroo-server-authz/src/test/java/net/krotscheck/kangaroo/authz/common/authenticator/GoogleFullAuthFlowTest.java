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

package net.krotscheck.kangaroo.authz.common.authenticator;

import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.IntegrationTest;
import net.krotscheck.kangaroo.test.TestConfig;
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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.or;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlContains;

/**
 * This test performs a series of full login flows against google.
 *
 * @author Michael Krotscheck
 */
@Category(IntegrationTest.class)
@RunWith(SingleInstanceTestRunner.class)
public final class GoogleFullAuthFlowTest
        extends AbstractBrowserLoginTest {

    /**
     * The selenium screenshot prefix.
     */
    private static final String S_PREFIX = "google";

    /**
     * Login page V1 flow.
     */
    private void googleLoginV1() {
        WebDriver d = SELENIUM.getDriver();

        By emailInput = By.id("identifierId");
        By nextButton = By.id("identifierNext");
        By passInput = By.cssSelector("div#password input[type='password']");
        By passNext = By.id("passwordNext");

        // Enter the login
        d.findElement(emailInput).clear();
        d.findElement(emailInput).sendKeys(TestConfig.getGoogleAccountId());
        d.findElement(nextButton).click();
        SELENIUM.screenshot(S_PREFIX);
        new WebDriverWait(d, TIMEOUT)
                .until(elementToBeClickable(passInput));

        // Enter the password
        d.findElement(passInput).clear();
        d.findElement(passInput).sendKeys(TestConfig.getGoogleAccountSecret());
        d.findElement(passNext).click();

        postLoginChecks();
    }

    /**
     * Login Page V2 flow.
     */
    private void googleLoginV2() {
        WebDriver d = SELENIUM.getDriver();
        SELENIUM.screenshot(S_PREFIX);

        By emailInput = By.id("Email");
        By nextButton = By.id("next");
        By passInput = By.id("Passwd");
        By signinButton = By.id("signIn");

        d.findElement(emailInput).clear();
        d.findElement(emailInput).sendKeys(TestConfig.getGoogleAccountId());
        d.findElement(nextButton).click();

        new WebDriverWait(d, TIMEOUT)
                .until(elementToBeClickable(passInput));
        d.findElement(passInput).clear();
        d.findElement(passInput).sendKeys(TestConfig.getGoogleAccountSecret());
        d.findElement(signinButton).click();

        postLoginChecks();
    }

    /**
     * Post-login checks for potential identity checks.
     */
    private void postLoginChecks() {
        WebDriver d = SELENIUM.getDriver();

        // Test for different conditions here.
        SELENIUM.screenshot(S_PREFIX);
        new WebDriverWait(d, TIMEOUT)
                .until(or(
                        urlContains("https://myaccount.google.com"),
                        urlContains("https://accounts.google.com/"
                                + "signin/oauth/oauthchooseaccount"),
                        urlContains("https://accounts.google.com/"
                                + "signin/v2/challenge/selection")
                ));

        // Test for the sign-in challenge
        if (d.getCurrentUrl().contains("https://accounts.google.com/"
                + "signin/v2/challenge/selection")) {
            SELENIUM.screenshot(S_PREFIX);
            d.findElement(By.cssSelector("[data-challengetype=16]")).click();

            new WebDriverWait(d, TIMEOUT)
                    .until(presenceOfElementLocated(
                            By.id("knowledgeLoginLocationInput")
                    ))
                    .sendKeys("Bellevue, WA");

            d.findElement(By.id("next")).click();

            new WebDriverWait(d, TIMEOUT)
                    .until(or(
                            urlContains("https://myaccount.google.com")
                    ));
        }
    }

    /**
     * Reset the facebook account before every test.
     */
    @Before
    public void googleLogin() {
        WebDriver d = SELENIUM.getDriver();

        // Hit the login page, then wait to see which page we've been given.
        d.get("https://accounts.google.com/ServiceLogin");
        SELENIUM.screenshot(S_PREFIX);
        new WebDriverWait(d, 10)
                .until(or(
                        elementToBeClickable(By.id("identifierId")),
                        elementToBeClickable(By.id("Email"))
                ));

        if (d.findElements(By.id("Email")).size() > 0) {
            googleLoginV2();
        } else {
            googleLoginV1();
        }
    }

    /**
     * Reset the google account before every test.
     */
    @After
    public void googleLogout() {
        WebDriver d = SELENIUM.getDriver();
        d.get("https://accounts.google.com/Logout");
        SELENIUM.screenshot(S_PREFIX);
        new WebDriverWait(d, TIMEOUT)
                .until(urlContains("https://accounts.google.com/"));
        SELENIUM.screenshot(S_PREFIX);
    }

    /**
     * Someone new to your app logs in with Google.
     */
    @Test
    public void testNewLogin() {
        String testState = RandomStringUtils.randomAlphanumeric(20);

        // Which account do you want to log in as page.
        String accountChooserUrl =
                "https://accounts.google.com/signin/oauth/oauthchooseaccount";
        ExpectedCondition<Boolean> accountChooser =
                urlContains(accountChooserUrl);

        // Google "do you want to grant access?" button.
        By approveAccessButton = By.id("submit_approve_access");
        ExpectedCondition<WebElement> approveOAuthApp =
                elementToBeClickable(approveAccessButton);

        // Success url.
        String exampleUrl = "www.example.com";
        ExpectedCondition<Boolean> successPage =
                urlContains("www.example.com");

        // Issue a request against our /authorize endpoint.
        URI requestUri = UriBuilder.fromUri(getBaseUri())
                .path("/authorize")
                .queryParam("authenticator", AuthenticatorType.Google)
                .queryParam("response_type", "code")
                .queryParam("client_id",
                        IdUtil.toString(getContext().getClient().getId()))
                .queryParam("scope", "debug")
                .queryParam("state", testState)
                .build();

        WebDriver d = SELENIUM.getDriver();
        d.get(requestUri.toString());
        SELENIUM.screenshot(S_PREFIX);

        // One of three things will happen.
        (new WebDriverWait(d, TIMEOUT))
                .until(or(accountChooser, approveOAuthApp, successPage));
        SELENIUM.screenshot(S_PREFIX);

        // First, did we hit the account chooser?
        if (d.getCurrentUrl().contains(accountChooserUrl)) {
            d.findElement(By.cssSelector("[data-profileindex=\"0\"]"))
                    .click();
            SELENIUM.screenshot(S_PREFIX);
        }

        // Wait for our other cases to resolve
        (new WebDriverWait(d, TIMEOUT)).until(or(approveOAuthApp, successPage));

        // Did we hit the approve app page?
        if (d.findElements(approveAccessButton).size() > 0) {
            d.findElement(approveAccessButton).click();
            SELENIUM.screenshot(S_PREFIX);
        }

        // Now we're expecting the success page.
        (new WebDriverWait(d, TIMEOUT)).until(successPage);
        SELENIUM.screenshot(S_PREFIX);

        String url = d.getCurrentUrl();
        URI uri = URI.create(url);
        MultivaluedMap<String, String> params = HttpUtil.parseQueryParams(uri);
        BigInteger code = IdUtil.fromString(params.getFirst("code"));
        assertEquals(testState, params.getFirst("state"));

        OAuthToken authToken = getSession().get(OAuthToken.class, code);
        assertEquals(authToken.getClient().getId(),
                getContext().getClient().getId());
        assertEquals(AuthenticatorType.Google,
                authToken.getIdentity().getType());
        assertEquals(OAuthTokenType.Authorization,
                authToken.getTokenType());
    }
}
