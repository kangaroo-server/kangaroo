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

package net.krotscheck.kangaroo.authz.oauth2.session.grizzly;

import net.krotscheck.kangaroo.authz.AuthzServerConfig;
import net.krotscheck.kangaroo.common.config.SystemConfiguration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 * Container lifecycle listener for the session feature, which grabs an
 * instance of the server and replaces session management with our own
 * implementation. This implementation is provided with full knowledge of the
 * Jersey2 injection context, including things like database connections, etc.
 *
 * @author Michael Krotscheck
 */
@Provider
@Singleton
public final class GrizzlySessionLifecycleListener
        implements ContainerLifecycleListener {

    /**
     * The grizzly webapplication context.
     */
    private final WebappContext context;

    /**
     * The injected & Constructed session manager.
     */
    private final GrizzlySessionManager sessionManager;

    /**
     * Max age, in seconds, of the session.
     */
    private final Integer sessionMaxAge;

    /**
     * The name of the session (aka cookie).
     */
    private final String sessionName;

    /**
     * Create a new instance of this lifecycle listener.
     *
     * @param context        The servlet context, expected to be Grizzly
     *                       WebappContext.
     * @param sessionManager The OAuth2 Session manager.
     * @param c              Global system configuration.
     */
    @Inject
    public GrizzlySessionLifecycleListener(
            @Context final ServletContext context,
            final GrizzlySessionManager sessionManager,
            final SystemConfiguration c) {
        this.context = (WebappContext) context;
        this.sessionManager = sessionManager;

        // Get the session name and expiry.
        sessionMaxAge = c.getInt(
                AuthzServerConfig.SESSION_MAX_AGE.getKey(),
                AuthzServerConfig.SESSION_MAX_AGE.getValue());
        sessionName = c.getString(
                AuthzServerConfig.SESSION_NAME.getKey(),
                AuthzServerConfig.SESSION_NAME.getValue());
    }

    /**
     * On container startup, replace our session manager.
     *
     * @param container The servlet container, not used.
     */
    @Override
    public void onStartup(final Container container) {
        context.setSessionManager(sessionManager);
        context.getSessionCookieConfig().setName(sessionName);
        context.getSessionCookieConfig().setHttpOnly(true);
        context.getSessionCookieConfig().setSecure(true);
        context.getSessionCookieConfig().setMaxAge(sessionMaxAge);
    }

    /**
     * On reload, do nothing.
     *
     * @param container The servlet container, not used.
     */
    @Override
    public void onReload(final Container container) {
    }

    /**
     * On shutdown, do nothing.
     *
     * @param container The servlet container, not used.
     */
    @Override
    public void onShutdown(final Container container) {

    }
}
