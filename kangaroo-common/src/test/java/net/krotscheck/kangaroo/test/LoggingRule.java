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

package net.krotscheck.kangaroo.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A JUnit rule, which permits selective capturing of logback log entries, based
 * on class and logging level.
 *
 * @author Michael Krotscheck
 */
public final class LoggingRule implements TestRule {

    /**
     * The target class onto which to attach the new appender.
     */
    private final Class targetClass;

    /**
     * The logging level to record at.
     */
    private final Level level;

    /**
     * The current active debug appender.
     */
    private DebugAppender activeAppender;

    /**
     * Create a new instance of this logging rule for a specific class.
     *
     * @param targetClass The class to target.
     * @param level       The logging level
     */
    public LoggingRule(final Class targetClass, final Level level) {
        this.targetClass = targetClass;
        this.level = level;
    }

    /**
     * Create a mock appender, start recording events, run the test, and then
     * detach.
     *
     * @param base        The base test.
     * @param description The test description.
     * @return The testing statement.
     */
    @Override
    public Statement apply(final Statement base,
                           final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                Logger classLogger = (Logger)
                        LoggerFactory.getLogger(targetClass);

                activeAppender = new DebugAppender();
                activeAppender.setContext(classLogger.getLoggerContext());
                activeAppender.start();
                Level old = classLogger.getLevel();
                try {
                    classLogger.addAppender(activeAppender);
                    classLogger.setLevel(level);
                    base.evaluate();
                } finally {
                    activeAppender.stop();
                    classLogger.detachAppender(activeAppender);
                    classLogger.setLevel(old);
                    activeAppender = null;
                }
            }
        };
    }

    /**
     * Clear the message buffer.
     */
    public void clear() {
        activeAppender.messages.clear();
    }

    /**
     * Get all the messages currently in the cache.
     *
     * @return All messages.
     */
    public List<String> getMessages() {
        return new ArrayList<>(activeAppender.messages);
    }

    /**
     * A test appender.
     *
     * @param <E> Event for the logger (Usually LoggingEvent).
     */
    private static class DebugAppender<E> extends AppenderBase<E> {

        /**
         * List of captured messages.
         */
        private final List<String> messages = new ArrayList<>();

        /**
         * Return the name.
         *
         * @return The test name of the debug appender.
         */
        @Override
        public String getName() {
            return "TEST";
        }

        /**
         * Take in an event object.
         *
         * @param eventObject The event to log.
         */
        @Override
        protected void append(final E eventObject) {
            ILoggingEvent event = (ILoggingEvent) eventObject;
            messages.add(event.getFormattedMessage());
        }
    }
}
