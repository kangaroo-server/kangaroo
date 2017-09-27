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

package net.krotscheck.kangaroo.common.response;

import net.krotscheck.kangaroo.common.hibernate.entity.AbstractEntity;

import java.util.List;

/**
 * This POJO describes an API list response. As it is insecure for us to
 * return raw arrays of data (See OWASP), we instead wrap the list response
 * in this pojo.
 *
 * @param <T> The data type returned.
 * @author Michael Krotscheck
 */
public final class ListResponseEntity<T extends AbstractEntity> {

    /**
     * The total # of records available.
     */
    private Number total;

    /**
     * The page offset.
     */
    private Number offset;

    /**
     * The page size limit.
     */
    private Number limit;

    /**
     * The sort key.
     */
    private String sort;

    /**
     * The sort order.
     */
    private SortOrder order;

    /**
     * The results.
     */
    private List<T> results;

    /**
     * The total # of records available.
     *
     * @return The total # of records available.
     */
    public Number getTotal() {
        return total;
    }

    /**
     * Set the total.
     *
     * @param total The new total.
     */
    protected void setTotal(final Number total) {
        this.total = total;
    }

    /**
     * The page offset.
     *
     * @return The page offset.
     */
    public Number getOffset() {
        return offset;
    }

    /**
     * Set the offset.
     *
     * @param offset The new offset.
     */
    protected void setOffset(final Number offset) {
        this.offset = offset;
    }

    /**
     * The page size limit.
     *
     * @return The page size limit.
     */
    public Number getLimit() {
        return limit;
    }

    /**
     * Set the limit.
     *
     * @param limit The new limit.
     */
    protected void setLimit(final Number limit) {
        this.limit = limit;
    }

    /**
     * The sort key.
     *
     * @return The sort key.
     */
    public String getSort() {
        return sort;
    }

    /**
     * Set the sort.
     *
     * @param sort The new sort.
     */
    protected void setSort(final String sort) {
        this.sort = sort;
    }

    /**
     * The sort order.
     *
     * @return The sort order.
     */
    public SortOrder getOrder() {
        return order;
    }

    /**
     * Set the order.
     *
     * @param order The new order.
     */
    protected void setOrder(final SortOrder order) {
        this.order = order;
    }

    /**
     * The results.
     *
     * @return The results.
     */
    public List<T> getResults() {
        return results;
    }

    /**
     * Set the results.
     *
     * @param results The new results.
     */
    protected void setResults(final List<T> results) {
        this.results = results;
    }
}
