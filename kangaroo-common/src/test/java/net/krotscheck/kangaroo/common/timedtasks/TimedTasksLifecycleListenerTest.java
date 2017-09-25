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

package net.krotscheck.kangaroo.common.timedtasks;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Unit tests for the manager of injected timer tasks.
 *
 * @author Michael Krotscheck
 */
@RunWith(PowerMockRunner.class)
public class TimedTasksLifecycleListenerTest {

    /**
     * The injector (This is a mockito mock).
     */
    private InjectionManager injectionManager;

    /**
     * The class under test.
     */
    private TimedTasksLifecycleListener listener;

    /**
     * Test setup.
     */
    @Before
    public void setupLifecycleListener() {
        injectionManager = Mockito.mock(InjectionManager.class);
        listener = new TimedTasksLifecycleListener(injectionManager);
    }

    /**
     * Ensure that injected timer tasks are run.
     *
     * @throws Exception should not be thrown.
     */
    @Test
    public void testBasicFunction() throws Exception {
        Container container = Mockito.mock(Container.class);
        List<RepeatingTask> tasks = new ArrayList<>();

        TestTask one = new TestTask(100);
        TestTask two = new TestTask(200);
        tasks.add(one);
        tasks.add(two);

        Mockito.doReturn(tasks)
                .when(injectionManager)
                .getAllInstances(RepeatingTask.class);

        listener.onStartup(container);
        Mockito.verifyNoMoreInteractions(container);
        Thread.sleep(201);
        listener.onShutdown(container);
        Mockito.verifyNoMoreInteractions(container);

        Assert.assertEquals(one.getTickCount(), 2);
        Assert.assertEquals(two.getTickCount(), 1);
    }

    /**
     * Assert that reload does nothing.
     */
    @Test
    public void onReload() {
        Container container = Mockito.mock(Container.class);
        listener.onReload(container);
        Mockito.verifyNoMoreInteractions(container);
    }


    /**
     * A task to test with.
     */
    public static final class TestTask
            extends TimerTask
            implements RepeatingTask {

        /**
         * The execution period.
         */
        private final long period;

        /**
         * The number of times this task was executed.
         */
        private long tickCount = 0;

        /**
         * Create a new test task.
         *
         * @param period The period of the task execution.
         */
        TestTask(final long period) {
            this.period = period;
        }

        /**
         * Return the tick count.
         *
         * @return The tick count.
         */
        public long getTickCount() {
            return tickCount;
        }

        /**
         * The task to execute.
         *
         * @return The task to execute.
         */
        @Override
        public TimerTask getTask() {
            return this;
        }

        /**
         * The timer period.
         */
        @Override
        public long getPeriod() {
            return period;
        }

        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            this.tickCount++;
        }
    }
}
