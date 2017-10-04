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

package net.krotscheck.kangaroo.authz.oauth2.session;

import net.krotscheck.kangaroo.authz.oauth2.session.grizzly.GrizzlySessionLifecycleListener;
import net.krotscheck.kangaroo.authz.oauth2.session.grizzly.GrizzlySessionManager;
import net.krotscheck.kangaroo.authz.oauth2.session.tasks.HttpSessionCleanupTask;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature overrides the container's session management with our own, so
 * that cookies may be manipulated and maintained by a DI-provided entity.
 * With that in place, it then provides the necessary processors and
 * injectors to provide access to session and client sensitive refresh tokens.
 *
 * @author Michael Krotscheck
 */
public final class SessionFeature implements Feature {

    /**
     * Register all associated features.
     *
     * @param context The context in which to register features.
     * @return true.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Convenience injector for the HTTP session.
        context.register(new HttpSessionFactory.Binder());

        // Cleanup all old sessions.
        context.register(new HttpSessionCleanupTask.Binder());

        // Grizzly session management
        context.register(GrizzlySessionLifecycleListener.class);
        context.register(new GrizzlySessionManager.Binder());

        return true;
    }
}
