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
import net.krotscheck.kangaroo.test.rule.FacebookTestUser;
import net.krotscheck.kangaroo.test.runner.SingleInstanceTestRunner;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.support.ui.ExpectedConditions.and;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

/**
 * This test performs a series of full login flows against facebook.
 *
 * @author Michael Krotscheck
 */
@Category(IntegrationTest.class)
@RunWith(SingleInstanceTestRunner.class)
public final class FacebookFullAuthFlowTest
        extends AbstractBrowserLoginTest {

    /**
     * The selenium screenshot prefix.
     */
    private static final String S_PREFIX = "facebook";

    /**
     * A facebook test user.
     */
    @ClassRule
    public static final FacebookTestUser FB_USER = new FacebookTestUser();

    /**
     * Reset the facebook account before every test.
     */
    @Before
    public void facebookLogin() {
        WebDriver d = SELENIUM.getDriver();

        // Login to facebook.
        d.get("https://www.facebook.com/");
        new WebDriverWait(d, TIMEOUT)
                .until(and(
                        elementToBeClickable(By.id("loginbutton")),
                        elementToBeClickable(By.id("email")),
                        elementToBeClickable(By.id("pass"))
                ));

        d.findElement(By.id("email")).clear();
        d.findElement(By.id("email")).sendKeys(FB_USER.getEmail());
        d.findElement(By.id("pass")).clear();
        d.findElement(By.id("pass")).sendKeys(FB_USER.getPassword());
        d.findElement(By.id("loginbutton")).click();
        new WebDriverWait(d, TIMEOUT)
                .until(presenceOfElementLocated(By.id("pagelet_welcome")));
    }

    /**
     * Reset the facebook account before every test.
     */
    @After
    public void facebookLogout() {
        WebDriver d = SELENIUM.getDriver();

        d.get("https://www.facebook.com/");
        WebElement loginAnchor = new WebDriverWait(d, TIMEOUT)
                .until(presenceOfElementLocated(
                        By.cssSelector("a#pageLoginAnchor")));

        // If the GDPR window exists, remove it
        String css = "[data-testid=\"gdpr_flow_dialog\"] "
                + "[data-testid=\"parent_deny_consent_button\"] button";
        List<WebElement> gdprCancel = d.findElements(By.cssSelector(css));
        if (gdprCancel.size() > 0) {
            gdprCancel.get(0).click();
        }

        loginAnchor.click();
        new WebDriverWait(d, TIMEOUT)
                .until(presenceOfElementLocated(
                        By.partialLinkText("Log Out")))
                .click();
        new WebDriverWait(d, TIMEOUT)
                .until(presenceOfElementLocated(
                        By.id("loginbutton")));
    }

    /**
     * 1. Someone new to your app logs in with Facebook
     * - Go to your app and tap on the Log in with Facebook button
     * - Tap OK to accept the read permissions
     * - Click OK again to accept write permissions if applicable
     * - Go to app settings and verify that the granted permissions are there
     */
    @Test
    public void testNewLogin() {
        String testState = RandomStringUtils.randomAlphanumeric(20);

        // Issue a request against our /authorize endpoint.
        URI requestUri = UriBuilder.fromUri(getBaseUri())
                .path("/authorize")
                .queryParam("authenticator", AuthenticatorType.Facebook)
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

        String url = d.getCurrentUrl();
        URI uri = URI.create(url);
        MultivaluedMap<String, String> params = HttpUtil.parseQueryParams(uri);
        BigInteger code = IdUtil.fromString(params.getFirst("code"));
        assertEquals(testState, params.getFirst("state"));

        OAuthToken authToken = getSession().get(OAuthToken.class, code);
        assertEquals(authToken.getClient().getId(),
                getContext().getClient().getId());
        assertEquals(AuthenticatorType.Facebook,
                authToken.getIdentity().getType());
        assertEquals(OAuthTokenType.Authorization,
                authToken.getTokenType());
    }
}
