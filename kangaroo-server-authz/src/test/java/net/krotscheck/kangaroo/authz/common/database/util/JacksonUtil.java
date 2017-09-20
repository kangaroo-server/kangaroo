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

package net.krotscheck.kangaroo.authz.common.database.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;

/**
 * Jackson utility.
 *
 * @author Michael Krotscheck
 */
public final class JacksonUtil {

    /**
     * Utility class, private constructor.
     */
    private JacksonUtil() {
    }

    /**
     * Generate an object mapper as used by the application itself.
     *
     * @return A new, simple object mapper.
     */
    public static ObjectMapper buildMapper() {
        InjectionManager manager = Injections.createInjectionManager();
        ObjectMapper mapper = new ObjectMapperFactory(manager).get();
        manager.shutdown();
        return mapper;
    }
}
