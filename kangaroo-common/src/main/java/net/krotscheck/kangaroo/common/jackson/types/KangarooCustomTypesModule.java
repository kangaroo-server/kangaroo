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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;

import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

/**
 * This Jackson Module registers all the necessary De/Serializers for proper
 * processing of our database entities.
 *
 * @author Michael Krotscheck
 */
@Provider
@Singleton
public final class KangarooCustomTypesModule extends Module {

    /**
     * The module name.
     */
    private final String name;

    /**
     * The module version (unknown).
     */
    private final Version version;

    /**
     * Constructor for this module.
     */
    public KangarooCustomTypesModule() {
        this.name = this.getClass().getSimpleName();
        this.version = Version.unknownVersion();
    }

    /**
     * Module name.
     *
     * @return The class name.
     */
    @Override
    public String getModuleName() {
        return this.name;
    }

    /**
     * The module version.
     *
     * @return The version.
     */
    @Override
    public Version version() {
        return this.version;
    }

    @Override
    public void setupModule(final SetupContext context) {
        SimpleDeserializers des = new SimpleDeserializers();
        des.addDeserializer(byte[].class, new Base16ByteDeserializer());

        SimpleSerializers ser = new SimpleSerializers();
        ser.addSerializer(byte[].class, new Base16ByteSerializer());

        context.addDeserializers(des);
        context.addSerializers(ser);
    }
}
