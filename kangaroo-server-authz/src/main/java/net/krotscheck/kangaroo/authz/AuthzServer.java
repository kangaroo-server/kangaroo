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

package net.krotscheck.kangaroo.authz;

import net.krotscheck.kangaroo.authz.admin.AdminV1API;
import net.krotscheck.kangaroo.authz.oauth2.OAuthAPI;
import net.krotscheck.kangaroo.server.ConfigurationBuilder;
import net.krotscheck.kangaroo.server.ServerFactory;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration.Configuration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;

/**
 * This is the main entrypoint to the Kangaroo Authz Administration Server.
 *
 * @author Michael Krotscheck
 */
public final class AuthzServer {

    /**
     * Our commandline options.
     */
    private static final Options CLI_OPTIONS;

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    static {
        // Initialize the CLI Options.
        CLI_OPTIONS = new Options();

        Option cookieName = Option.builder("cn")
                .longOpt(AuthzServerConfig.SESSION_NAME.getKey())
                .argName(AuthzServerConfig.SESSION_NAME.getKey())
                .hasArg()
                .desc("The cookie name to use, default 'kangaroo'.")
                .build();

        Option cookieExpiresIn = Option.builder("ce")
                .longOpt(AuthzServerConfig.SESSION_MAX_AGE.getKey())
                .argName(AuthzServerConfig.SESSION_MAX_AGE.getKey())
                .hasArg()
                .desc("The maxium age of the cookie in seconds. Default 1 day.")
                .build();

        CLI_OPTIONS.addOption(cookieName);
        CLI_OPTIONS.addOption(cookieExpiresIn);
    }

    /**
     * Utility class, private constructor.
     */
    private AuthzServer() {
    }

    /**
     * Launch the Authz Admin Server.
     *
     * @param args Commandline arguments passed during launch.
     * @throws IOException          Exception thrown if we cannot start up
     *                              the HTTP stack.
     * @throws InterruptedException Thread interruption.
     */
    public static void main(final String[] args)
            throws IOException, InterruptedException {

        Configuration config = new ConfigurationBuilder()
                .withCommandlineOptions(CLI_OPTIONS)
                .addCommandlineArgs(args)
                .addPropertiesFile("kangaroo.authz.properties")
                .build();

        Integer sessionMaxAge = config.getInt(
                AuthzServerConfig.SESSION_MAX_AGE.getKey(),
                AuthzServerConfig.SESSION_MAX_AGE.getValue());

        HttpServer server = new ServerFactory()
                .withCommandlineOptions(CLI_OPTIONS)
                .withCommandlineArgs(args)
                .withPropertiesFile("kangaroo.authz.properties")
                .withResource("/v1", new AdminV1API())
                .withResource("/oauth2", new OAuthAPI())
                .configureServer((s) -> {
                    ServerConfiguration c = s.getServerConfiguration();
                    c.setSessionTimeoutSeconds(sessionMaxAge);
                })
                .build();

        Runtime.getRuntime()
                .addShutdownHook(new Thread(server::shutdownNow, "shutdown"));

        server.start();
        Thread.currentThread().join();
    }
}
