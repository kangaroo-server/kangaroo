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

package net.krotscheck.kangaroo;

import net.krotscheck.kangaroo.server.ServerFactory;
import net.krotscheck.kangaroo.servlet.admin.v1.AdminV1API;
import org.glassfish.grizzly.http.server.HttpServer;

import java.io.IOException;

/**
 * This is the main entrypoint to the Kangaroo Authz Administration Server.
 *
 * @author Michael Krotscheck
 */
public final class AuthzAdminServer {

    /**
     * Utility class, private constructor.
     */
    private AuthzAdminServer() {
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

        HttpServer server = new ServerFactory()
                .withCommandlineArgs(args)
                .withPropertiesFile("kangaroo.admin.properties")
                .withResource("/v1", new AdminV1API())
                .build();

        Runtime.getRuntime()
                .addShutdownHook(new Thread(server::shutdownNow, "shutdown"));

        server.start();
        Thread.currentThread().join();
    }
}
