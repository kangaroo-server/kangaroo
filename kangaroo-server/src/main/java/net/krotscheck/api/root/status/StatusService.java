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

package net.krotscheck.api.root.status;


import net.krotscheck.kangaroo.common.config.SystemConfiguration;
import org.apache.http.HttpStatus;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A simple endpoint that returns the system status.
 *
 * @author Michael Krotscheck
 */
@Path("/")
@PermitAll
public final class StatusService {

    /**
     * The system configuration from which to read status features.
     */
    private SystemConfiguration config;

    /**
     * Create a new instance of the status service.
     *
     * @param config Injected system configuration.
     */
    @Inject
    public StatusService(final SystemConfiguration config) {
        this.config = config;
    }

    /**
     * Always returns the version.
     *
     * @return HTTP Response object with the current service status.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        StatusResponse status = new StatusResponse();
        status.setVersion(config.getVersion());

        return Response.status(HttpStatus.SC_OK).entity(status).build();
    }
}
