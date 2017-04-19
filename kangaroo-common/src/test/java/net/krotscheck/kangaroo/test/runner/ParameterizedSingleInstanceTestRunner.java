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

package net.krotscheck.kangaroo.test.runner;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

import java.util.List;

/**
 * A parameterized test runner which only creates a single instance of the test
 * class for all executions, rather than an instance per execution.
 * <p>
 * This is a straight copy/paste of the BlockJUnit4ClassRunnerWithParameters
 * class, which unfortunately isn't that extensible. If something breaks
 * here, copy/paste the originating class and see if that does it.
 *
 * @author Michael Krotscheck
 */
public final class ParameterizedSingleInstanceTestRunner
        extends BlockJUnit4ClassRunner {

    /**
     * The test parameters.
     */
    private final Object[] parameters;

    /**
     * The name of the test.
     */
    private final String name;

    /**
     * Create a new instance of this test runner.
     *
     * @param test The test to run.
     * @throws InitializationError Thrown if the test class is malformed.
     */
    public ParameterizedSingleInstanceTestRunner(final TestWithParameters test)
            throws InitializationError {
        super(test.getTestClass().getJavaClass());
        parameters = test.getParameters().toArray(
                new Object[test.getParameters().size()]);
        name = test.getName();
    }

    /**
     * Create a new test, with the parameters captured.
     *
     * @return A new test instance.
     * @throws Exception Thrown if we cannot instantiate the test.
     */
    @Override
    public Object createTest() throws Exception {
        return TestInstanceManager.getInstance(getTestClass(),
                getChildren().size(), parameters);
    }

    @Override
    protected void validateConstructor(final List<Throwable> errors) {
        validateOnlyOneConstructor(errors);
    }

    /**
     * Return the test name.
     *
     * @return The test name.
     */
    @Override
    protected String getName() {
        return name;
    }

    /**
     * Return the method name.
     *
     * @param method The method to name.
     * @return The method test name.
     */
    @Override
    protected String testName(final FrameworkMethod method) {
        return method.getName() + getName();
    }

    /**
     * Return the class block.
     *
     * @param notifier The run notifier for this class block.
     * @return The run block.
     */
    @Override
    protected Statement classBlock(final RunNotifier notifier) {
        return childrenInvoker(notifier);
    }

    public static final class ParameterizedSingleInstanceTestRunnerFactory
            implements ParametersRunnerFactory {
        /**
         * Returns a runner for the specified {@link TestWithParameters}.
         *
         * @param test
         * @throws InitializationError if the runner could not be created.
         */
        @Override
        public Runner createRunnerForTestWithParameters(
                final TestWithParameters test) throws InitializationError {
            return new ParameterizedSingleInstanceTestRunner(test);
        }
    }
}
