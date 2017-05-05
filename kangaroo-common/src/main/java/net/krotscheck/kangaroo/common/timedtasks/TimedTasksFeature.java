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


import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature collects, and starts, any number of timed tasks that it finds
 * within the injection system.
 *
 * @author Michael Krotscheck
 */
public final class TimedTasksFeature implements Feature {

    /**
     * Register the TimedTasksFeature with the current application context.
     *
     * @param context The application context.
     * @return Always true.
     */
    @Override
    public boolean configure(final FeatureContext context) {
        context.register(new TimedTasksLifecycleListener.Binder());
        return true;
    }
}
