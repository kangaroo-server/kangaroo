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
public final class ApiParam {

    /**
     * Private constructor for utility class.
     */
    private ApiParam() {
    }

    /**
     * Header name: Sort On.
     */
    public static final String SORT_HEADER = "Sort";

    /**
     * Query Variable: Sort On.
     */
    public static final String SORT_QUERY = "sort";

    /**
     * Default Value: Sort On.
     */
    public static final String SORT_DEFAULT = "createdDate";

    /**
     * Header name: List Order.
     */
    public static final String ORDER_HEADER = "Order";

    /**
     * Query variable: List Order.
     */
    public static final String ORDER_QUERY = "order";

    /**
     * Default Value: List Order.
     */
    public static final String ORDER_DEFAULT = "ASC";

    /**
     * Header name: List Offset.
     */
    public static final String OFFSET_HEADER = "Offset";

    /**
     * Query variable: List Offset.
     */
    public static final String OFFSET_QUERY = "offset";

    /**
     * Default value: List Offset.
     */
    public static final String OFFSET_DEFAULT = "0";

    /**
     * Header name: Search/Browse Paging Limit.
     */
    public static final String LIMIT_HEADER = "Limit";

    /**
     * Query name: Search/Browse Paging Limit.
     */
    public static final String LIMIT_QUERY = "limit";

    /**
     * Default Value: Search/Browse Paging Limit.
     */
    public static final String LIMIT_DEFAULT = "10";

    /**
     * The total number of results.
     */
    public static final String TOTAL_HEADER = "Total";

    /**
     * Header Name: Query String.
     */
    public static final String QUERY_HEADER = "Query";

    /**
     * Query variable: Query String.
     */
    public static final String QUERY_QUERY = "q";

    /**
     * Default value: Query String.
     */
    public static final String QUERY_DEFAULT = "";

}
