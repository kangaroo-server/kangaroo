/*
 * Copyright (c) 2016 Michael Krotscheck
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
 */

package net.krotscheck.status;

import net.krotscheck.features.config.ConfigurationFeature;
import net.krotscheck.status.features.status.StatusFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by krotscheck on 4/13/16.
 */
public class StatusAPI extends ResourceConfig {

    /**
     * Constructor. Creates a new application instance.
     */
    public StatusAPI() {
        // Configuration loader
        register(ConfigurationFeature.class);
        register(StatusFeature.class);
    }

}
