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

package net.krotscheck.features.response;

/**
 * Collection of HTTP Headers that are used by the List Response Builder.
 *
 * @author Michael Krotscheck
 */
public enum SortOrder {

    /**
     * Sort in Ascending Order.
     */
    ASC("ASC"),

    /**
     * Sort in Descending Order.
     */
    DESC("DESC");

    /**
     * The string value of this enum.
     */
    private final String value;

    /**
     * Create a new instance of this enum.
     *
     * @param value The value of the enum.
     */
    SortOrder(final String value) {
        this.value = value;
    }

    /**
     * Convert a string into an Enum instance.
     *
     * @param value The value to interpret.
     * @return Either ASC or DESC, default ASC.
     */
    public static SortOrder fromString(final String value) {
        if (DESC.value.equals(value.toUpperCase())) {
            return DESC;
        } else {
            return ASC;
        }
    }

    /**
     * Return the string representation of this enum.
     *
     * @return The string representation.
     */
    public String toString() {
        return value;
    }

}
