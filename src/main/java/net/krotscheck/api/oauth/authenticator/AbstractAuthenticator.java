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

package net.krotscheck.api.oauth.authenticator;

import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidRequestException;

import java.util.List;
import javax.ws.rs.core.MultivaluedMap;

/**
 * An abstract authenticator, mostly containing utility methods.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractAuthenticator implements IAuthenticator {

    /**
     * This helper method assumes at least, but no more than, one single
     * value in a MultiValuedMap. If this value exists, it is returned.
     * Otherwise, it raises an InvalidRequestException.
     *
     * @param values The map of values.
     * @param key    The key to get.
     * @return The value retrieved, but only if only one exists for this key.
     * @throws InvalidRequestException Thrown if the value is
     *                                 not found, or if more than one instance
     *                                 of the value is
     *                                 found.
     */
    protected final String getOne(final MultivaluedMap<String, String> values,
                                  final String key)
            throws InvalidRequestException {
        List<String> listValues = values.get(key);
        if (listValues == null) {
            throw new InvalidRequestException();
        }
        if (listValues.size() != 1) {
            throw new InvalidRequestException();
        }
        return listValues.get(0);
    }
}
