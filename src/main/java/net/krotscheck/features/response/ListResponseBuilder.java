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

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

/**
 * Convenience utility to build our common list response objects.
 *
 * @author Michael Krotscheck
 */
public final class ListResponseBuilder {

    /**
     * The wrapped builder.
     */
    private Response.ResponseBuilder builder = Response.ok();

    /**
     * The list of headers which have been added.
     */
    private final List<String> addedHeaders = new ArrayList<>();

    /**
     * Create a new personal response builder.
     *
     * @return The response builder.
     */
    public static ListResponseBuilder builder() {
        return new ListResponseBuilder();
    }

    /**
     * Add a list or results to the response.
     *
     * @param results Some results.
     * @return The builder.
     */
    public ListResponseBuilder addResult(final List<?> results) {
        builder.entity(results);
        return this;
    }

    /**
     * Add a limit to the response.
     *
     * @param limit The limit.
     * @return The builder.
     */
    public ListResponseBuilder limit(final Number limit) {
        builder.header(HttpHeaders.LIMIT, limit.longValue());
        addedHeaders.add(HttpHeaders.LIMIT);
        return this;
    }

    /**
     * Add a total to the response.
     *
     * @param total The total.
     * @return The builder.
     */
    public ListResponseBuilder total(final Number total) {
        builder.header(HttpHeaders.TOTAL, total.longValue());
        addedHeaders.add(HttpHeaders.TOTAL);
        return this;
    }

    /**
     * Add an offset to the response.
     *
     * @param offset The offset.
     * @return The builder.
     */
    public ListResponseBuilder offset(final Number offset) {
        builder.header(HttpHeaders.OFFSET, offset.longValue());
        addedHeaders.add(HttpHeaders.OFFSET);
        return this;
    }

    /**
     * Add a sort to the response.
     *
     * @param sort The sort.
     * @return The builder.
     */
    public ListResponseBuilder sort(final String sort) {
        builder.header(HttpHeaders.SORT_ON, sort);
        addedHeaders.add(HttpHeaders.SORT_ON);
        return this;
    }

    /**
     * Add a sort order to the response.
     *
     * @param order The order.
     * @return The builder.
     */
    public ListResponseBuilder order(final SortOrder order) {
        builder.header(HttpHeaders.SORT_ORDER, order.toString());
        addedHeaders.add(HttpHeaders.SORT_ORDER);
        return this;
    }

    /**
     * Add a sort order to the response.
     *
     * @param order The order.
     * @return The builder.
     */
    public ListResponseBuilder order(final String order) {
        return this.order(SortOrder.fromString(order));
    }

    /**
     * Add a search query to the response.
     *
     * @param query The search query to add.
     * @return This builder.
     */
    public ListResponseBuilder query(final String query) {
        builder.header(HttpHeaders.QUERY, query);
        addedHeaders.add(HttpHeaders.QUERY);
        return this;
    }

    /**
     * Build the response.
     *
     * @return The response.
     */
    public Response build() {
        // Apply the vary headers
        builder.header("Vary", String.join(",", addedHeaders));
        return builder.build();
    }

    /**
     * Collection of HTTP Headers that are used by the List Response Builder.
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

    /**
     * Collection of HTTP Headers that are used by the List Response Builder.
     */
    public static final class HttpHeaders {

        /**
         * Private constructor, this is only for constants.
         */
        private HttpHeaders() {

        }

        /**
         * HTTP Header for the sort parameter.
         */
        public static final String SORT_ON = "Sort-On";

        /**
         * HTTP Header for the sort order.
         */
        public static final String SORT_ORDER = "Sort-Order";

        /**
         * HTTP Header the for the page offset.
         */
        public static final String OFFSET = "Offset";

        /**
         * HTTP Header for the paging limit.
         */
        public static final String LIMIT = "Limit";

        /**
         * HTTP Header for the total available number of records.
         */
        public static final String TOTAL = "Total";

        /**
         * HTTP Header for the search query string.
         */
        public static final String QUERY = "Query";

    }
}
