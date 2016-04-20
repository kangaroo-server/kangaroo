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

package net.krotscheck.features.jackson.mock;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A mock pojo.
 *
 * @author Michael Krotscheck
 */
@JsonDeserialize(using = MockPojoDeserializer.class)
@JsonSerialize(using = MockPojoSerializer.class)
public final class MockPojo {

    /**
     * This value is used to track the pojo's path through the deserialization
     * chain.
     */
    private boolean invokedDeserializer = false;

    /**
     * This value is used to track the pojo's path through the serialization
     * chain.
     */
    private boolean invokedSerializer = false;

    /**
     * Service data.
     */
    private boolean serviceData = false;

    /**
     * Has the deserialization flag been toggled?
     *
     * @return True if it's toggled, otherwise false.
     */
    public boolean isInvokedDeserializer() {
        return invokedDeserializer;
    }

    /**
     * Toggle the deserialization flag.
     *
     * @param invokedDeserializer True or false.
     */
    public void setInvokedDeserializer(final boolean invokedDeserializer) {
        this.invokedDeserializer = invokedDeserializer;
    }

    /**
     * Has the serialization flag been toggled?
     *
     * @return True if it's toggled, otherwise false.
     */
    public boolean isInvokedSerializer() {
        return invokedSerializer;
    }

    /**
     * Toggle the serialization flag.
     *
     * @param invokedSerializer True or false.
     */
    public void setInvokedSerializer(final boolean invokedSerializer) {
        this.invokedSerializer = invokedSerializer;
    }

    /**
     * Has the serviceData flag been toggled?
     *
     * @return True if it's toggled, otherwise false.
     */
    public boolean isServiceData() {
        return serviceData;
    }

    /**
     * Toggle the service data flag.
     *
     * @param serviceData True or false.
     */
    public void setServiceData(final boolean serviceData) {
        this.serviceData = serviceData;
    }
}
