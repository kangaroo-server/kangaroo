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

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
     * A running count of the # of screenshots we took in each namespcae.
     */
    private Map<String, Integer> scCount = new HashMap<>();

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
     * Test helper, permits ad-hoc creation of screenshots.
     *
     * @param prefix The screenshot prefix name, to permit easy sequencing.
     */
    public void screenshot(final String prefix) {
        Integer idx = scCount.getOrDefault(prefix, 0);
        File scrFile = driver.getScreenshotAs(OutputType.FILE);
        String newFileName = String.format("%s-%s-%s",
                prefix, idx, scrFile.getName());

        try {
            FileUtils.copyFile(scrFile,
                    new File("target/screenshots/" + newFileName));
        } catch (Exception e) {
            // Silence exceptions, this is a testing utility.
        } finally {
            scCount.put(prefix, idx + 1);
        }
    }

    /**
     * Start the rule.
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
                    chromeOptions.setHeadless(GraphicsEnvironment.isHeadless());
                    chromeOptions.addArguments("--disable-notifications");
                    chromeOptions.addArguments("--window-size=1920,1080");
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

    /**
     * A debugging tool, which dumps the current DOM tree to the console.
     */
    public void dumpHTML() {
        String pageSource = driver.getPageSource();
        System.out.print(pageSource);
    }
}
