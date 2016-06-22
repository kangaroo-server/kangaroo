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
        String headerName = ApiParam.LIMIT_HEADER;
        builder.header(headerName, limit.longValue());
        addedHeaders.add(headerName);
        return this;
    }

    /**
     * Add a total to the response.
     *
     * @param total The total.
     * @return The builder.
     */
    public ListResponseBuilder total(final Number total) {
        String headerName = ApiParam.TOTAL_HEADER;
        builder.header(headerName, total.longValue());
        addedHeaders.add(headerName);
        return this;
    }

    /**
     * Add a total to the response.
     *
     * @param total The total.
     * @return The builder.
     */
    public ListResponseBuilder total(final Object total) {
        String headerName = ApiParam.TOTAL_HEADER;
        builder.header(headerName, Long.valueOf(total.toString()));
        addedHeaders.add(headerName);
        return this;
    }

    /**
     * Add an offset to the response.
     *
     * @param offset The offset.
     * @return The builder.
     */
    public ListResponseBuilder offset(final Number offset) {
        String headerName = ApiParam.OFFSET_HEADER;
        builder.header(headerName, offset.longValue());
        addedHeaders.add(headerName);
        return this;
    }

    /**
     * Add a sort to the response.
     *
     * @param sort The sort.
     * @return The builder.
     */
    public ListResponseBuilder sort(final String sort) {
        String headerName = ApiParam.SORT_HEADER;
        builder.header(headerName, sort);
        addedHeaders.add(headerName);
        return this;
    }

    /**
     * Add a sort order to the response.
     *
     * @param order The order.
     * @return The builder.
     */
    public ListResponseBuilder order(final SortOrder order) {
        String headerName = ApiParam.ORDER_HEADER;
        builder.header(headerName, order.toString());
        addedHeaders.add(headerName);
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
        String headerName = ApiParam.QUERY_HEADER;
        builder.header(headerName, query);
        addedHeaders.add(headerName);
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
}
