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

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Timer;

/**
 * This lifecycle listener ensures that timed tasks discovered in the service
 * locator are started.
 *
 * @author Michael Krotscheck
 */
public final class TimedTasksLifecycleListener
        implements ContainerLifecycleListener {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(TimedTasksLifecycleListener.class);

    /**
     * The injector.
     */
    private InjectionManager injector;

    /**
     * The timer.
     */
    private Timer timer;

    /**
     * Create a new lifecycle listener.
     *
     * @param injector The injector from which to resolve tasks.
     */
    @Inject
    public TimedTasksLifecycleListener(final InjectionManager injector) {
        this.injector = injector;
    }

    /**
     * Start all timed tasks.
     *
     * @param container container that has been started.
     */
    @Override
    public void onStartup(final Container container) {
        logger.info("Starting timer");
        timer = new Timer(true);

        List<RepeatingTask> tasks =
                injector.getAllInstances(RepeatingTask.class);
        for (RepeatingTask task : tasks) {
            logger.debug(
                    String.format(
                            "Scheduling: %s: %s seconds",
                            task.getClass().getSimpleName(),
                            Math.floor(task.getPeriod() / 1000)
                    ));
            timer.scheduleAtFixedRate(task.getTask(), task.getDelay(),
                    task.getPeriod());
        }
    }

    /**
     * Invoked when the {@link Container container} has been reloaded.
     *
     * @param container container that has been reloaded.
     */
    @Override
    public void onReload(final Container container) {
    }

    /**
     * Stop all the timed tasks.
     *
     * @param container container that has been shut down.
     */
    @Override
    public void onShutdown(final Container container) {
        logger.info("Cancelling timer");
        timer.cancel();
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {
        @Override
        protected void configure() {
            bind(TimedTasksLifecycleListener.class)
                    .to(ContainerLifecycleListener.class)
                    .in(Singleton.class);
        }
    }
}
