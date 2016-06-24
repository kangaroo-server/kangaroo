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

package net.krotscheck.api.oauth.resource.grant;

import net.krotscheck.api.oauth.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.database.entity.Client;

import javax.ws.rs.core.MultivaluedMap;

/**
 * This interface describes a processing entity which encapsulates some logic
 * for a specific token grant_type.
 *
 * @author Michael Krotscheck
 */
public interface IGrantTypeHandler {

    /**
     * Handle a specific grant type request.
     *
     * @param client   The client.
     * @param formData Additional form data.
     * @return A token response entity with the new token.
     */
    TokenResponseEntity handle(Client client,
                               MultivaluedMap<String, String> formData);
}
