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

import com.google.common.io.Files;
import net.krotscheck.kangaroo.server.Config;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;

/**
 * This JUnit4 Rule creates, and tears down, a kangaroo 'working' directory,
 * and updates the necessary systme properties.
 *
 * @author Michael Krotscheck
 */
public final class WorkingDirectoryRule implements TestRule {

    /**
     * The working directory.
     */
    private File workingDir;

    /**
     * Get the working directory.
     *
     * @return The working dir, guaranteed to exist and be unique.
     */
    public File getWorkingDir() {
        return workingDir;
    }

    /**
     * Modifies the method-running {@link Statement} to implement this
     * test-running rule.
     *
     * @param base        The {@link Statement} to be modified
     * @param description A {@link Description} of the test implemented in
     *                    {@code base}
     * @return a new statement, which may be the same as {@code base},
     * a wrapper around {@code base}, or a completely new Statement.
     */
    @Override
    public Statement apply(final Statement base,
                           final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                workingDir = Files.createTempDir();
                System.setProperty(Config.WORKING_DIR.getKey(),
                        workingDir.getAbsolutePath());
                try {
                    // Evaluate the next round of tests.
                    base.evaluate();
                } finally {
                    System.clearProperty(Config.WORKING_DIR.getKey());
                    FileUtils.deleteDirectory(workingDir);
                }

            }
        };
    }
}
