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
 * An explicit list of common query parameters that apply to most of our API's.
 *
 * @author Michael Krotscheck
 */
public enum ApiParam {

    /**
     * List queries, on what field should we sort our response?
     */
    SORT("Sort", "sort", "id"),

    /**
     * The sort order, ASC or DESC.
     */
    ORDER("Order", "order", "ASC"),

    /**
     * The paging offset, in records.
     */
    OFFSET("Offset", "offset", "0"),

    /**
     * The paging limit, in records.
     */
    LIMIT("Limit", "limit", "10"),

    /**
     * The total number of results.
     */
    TOTAL("Total", null, null),

    /**
     * The search query string.
     */
    QUERY("Query", "q", "");

    /**
     * The HTTP header name.
     */
    private final String header;

    /**
     * The parameter's representation in the query string.
     */
    private final String queryParam;

    /**
     * The default value for this query parameter.
     */
    private final String queryDefault;

    /**
     * Get the text header for this query parameter.
     *
     * @return The name of the header.
     */
    public String headerName() {
        return header;
    }

    /**
     * Get the query parameter name for this param.
     *
     * @return The query param
     */
    public String queryParam() {
        return queryParam;
    }

    /**
     * Get the default value for this parameter.
     *
     * @return The default value.
     */
    public String queryDefault() {
        return queryDefault;
    }

    /**
     * Create a new API parameter, represented via headers and/or query
     * strings.
     *
     * @param header       The HTTP Header which represents this parameter.
     * @param queryParam   The query parameter name.
     * @param queryDefault The default value for the query parameter.
     */
    ApiParam(final String header,
             final String queryParam,
             final String queryDefault) {
        this.header = header;
        this.queryParam = queryParam;
        this.queryDefault = queryDefault;
    }
}
