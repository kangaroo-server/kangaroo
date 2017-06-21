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

package net.krotscheck.kangaroo.common.cors;

import java.net.URI;

/**
 * This interface describes an injected component, which should be provided
 * by each service itself, that can validate in a synchronous fashion whether
 * a particular Origin header is valid for a CORS request.
 *
 * @author Michael Krotscheck
 */
public interface ICORSValidator {

    /**
     * Return true if the URI is a valid origin for a CORS request.
     *
     * @param origin The origin to validate. Will only contain scheme, host,
     *               and port.
     * @return True if the origin is valid, otherwise false.
     */
    boolean isValidCORSOrigin(URI origin);
}
