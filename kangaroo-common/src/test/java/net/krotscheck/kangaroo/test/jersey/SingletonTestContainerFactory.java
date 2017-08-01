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

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class provides a singleton test container for a jersey test.
 *
 * @author Michael Krotscheck
 */
public class SingletonTestContainerFactory implements TestContainerFactory {

    /**
     * Wrapped factory, used to generate instances if we don't have one yet.
     */
    private final TestContainerFactory defaultFactory;

    /**
     * The number of expected tests.
     */
    private int testCount = 0;

    /**
     * List of test containers that have been generated during this run.
     */
    private TestContainer currentContainer;

    /**
     * Create a new singleton test container factory, wrapped around an
     * existing factory type.
     *
     * @param defaultFactory The factory to wrap.
     * @param testClass      The test class this instance
     *                       has been created for. Used to find the expected
     *                       # of tests.
     */
    public SingletonTestContainerFactory(
            final TestContainerFactory defaultFactory,
            final Class testClass) {
        this.defaultFactory = defaultFactory;

        List<Method> methods = getMethodsAnnotatedWith(testClass,
                Test.class);
        testCount = methods.size();
    }

    /**
     * This utility method extracts all methods annotated with a particular
     * type. We use it to find all tests in a suite, so we know when to shut
     * down the singleton.
     *
     * @param type       The type to scan.
     * @param annotation The annotation to look for.
     * @return A list of all methods with this type.
     */
    public static List<Method> getMethodsAnnotatedWith(
            final Class<?> type,
            final Class<? extends Annotation> annotation) {
        final List<Method> methods = new ArrayList<Method>();
        Class<?> klass = type;
        while (klass != Object.class) {

            final List<Method> allMethods =
                    new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(annotation)) {
                    methods.add(method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    /**
     * Create a test container instance.
     *
     * @param baseUri           base URI for the test container to run at.
     * @param deploymentContext deployment context of the tested JAX-RS / Jersey application .
     * @return new test container configured to run the tested application.
     * @throws IllegalArgumentException if {@code deploymentContext} is not supported
     *                                  by this test container factory.
     */
    @Override
    public TestContainer create(final URI baseUri,
                                final DeploymentContext deploymentContext) {
        if (currentContainer == null) {
            TestContainer created = defaultFactory.create(baseUri,
                    deploymentContext);
            currentContainer = new SingletonTestContainer(created, testCount);
        }

        return currentContainer;
    }

    /**
     * Wrapper test container, ensures that a container is only started once.
     */
    private static class SingletonTestContainer implements TestContainer {

        /**
         * How often are we expecting this container to be started?
         */
        private final int totalStartsExpected;
        /**
         * The wrapped testcontainer.
         */
        private final TestContainer wrapped;
        /**
         * How often has this container been started?
         */
        private int startRequests = 0;
        /**
         * Have we already started this container?
         */
        private boolean started = false;

        /**
         * Create a new SingletonTestContainer, wrapping an existing one.
         *
         * @param wrapped             The container to wrap.
         * @param totalStartsExpected The total number of
         *                            start() requests we're
         *                            expecting.
         */
        SingletonTestContainer(final TestContainer wrapped,
                               final int totalStartsExpected) {
            this.wrapped = wrapped;
            this.totalStartsExpected = totalStartsExpected;
        }

        /**
         * Get a client configuration specific to the test container.
         *
         * @return a client configuration specific to the test container, otherwise {@code null} if there
         * is no specific client configuration required.
         */
        @Override
        public ClientConfig getClientConfig() {
            return wrapped.getClientConfig();
        }

        /**
         * Get the base URI of the application.
         *
         * @return the base URI of the application.
         */
        @Override
        public URI getBaseUri() {
            return wrapped.getBaseUri();
        }

        /**
         * Start the container.
         */
        @Override
        public void start() {
            startRequests++;
            if (!started) {
                wrapped.start();
                started = true;
            }
        }

        /**
         * Stop the container, but only if it's been started the expected
         * number of times.
         */
        @Override
        public void stop() {
            if (startRequests == totalStartsExpected) {
                wrapped.stop();
                started = false;
            }
        }
    }
}
