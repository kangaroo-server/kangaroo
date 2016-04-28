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

package net.krotscheck.api.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The status resource is a POJO that encapsulates common API metadata.
 *
 * @author Michael Krotscheck
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StatusResponse {

    /**
     * The API version.
     */
    private String version = "";

    /**
     * Get the current API version.
     *
     * @return The released package version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the API version.
     *
     * @param version The version to set.
     */
    public void setVersion(final String version) {
        this.version = version;
    }
}
