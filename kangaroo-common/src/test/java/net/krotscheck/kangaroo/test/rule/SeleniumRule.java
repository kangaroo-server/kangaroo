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

package net.krotscheck.kangaroo.test.rule;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * This JUnit4 rule catpures and harnesses a chrome browser for direct testing.
 *
 * @author Michael Krotscheck
 */
public final class SeleniumRule implements TestRule {

    /**
     * The web driver for this rule.
     */
    private ChromeDriver driver;

    /**
     * Create a new selenium chrome rule.
     */
    public SeleniumRule() {
    }

    /**
     * Return the chrome driver.
     *
     * @return The driver.
     */
    public ChromeDriver getDriver() {
        return driver;
    }

    /**
     * Start the rule
     *
     * @param base
     * @param description
     * @return
     */
    @Override
    public Statement apply(final Statement base,
                           final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {

                try {
                    final ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.setHeadless(true);
                    driver = new ChromeDriver(chromeOptions);

                    base.evaluate();
                } finally {
                    if (driver != null) {
                        driver.close();
                    }
                }
            }
        };
    }
}
