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

package net.krotscheck.kangaroo.common.jackson.types;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;

import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Base16 serializer for byte arrays.
 *
 * @author Michael Krotscheck
 */
@Provider
@Singleton
public final class Base16ByteSerializer
        extends JsonSerializer<byte[]> {

    @Override
    public void serialize(final byte[] value,
                          final JsonGenerator gen,
                          final SerializerProvider serializers)
            throws IOException, JsonProcessingException {
        gen.writeString(IdUtil.toString(value));
    }
}
