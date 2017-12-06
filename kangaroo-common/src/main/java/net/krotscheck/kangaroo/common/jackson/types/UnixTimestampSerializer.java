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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Calendar;

/**
 * Second-precision serializer for UTC Calendar timestamps.
 *
 * @author Michael Krotscheck
 */
@Provider
@Singleton
public final class UnixTimestampSerializer
        extends JsonSerializer<Calendar> {

    @Override
    public void serialize(final Calendar value,
                          final JsonGenerator gen,
                          final SerializerProvider serializers)
            throws IOException {
        gen.writeNumber(value.getTimeInMillis() / 1000);
    }
}
