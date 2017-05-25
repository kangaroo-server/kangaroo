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

package net.krotscheck.kangaroo.authz.common.database.util;

import net.krotscheck.kangaroo.common.response.SortOrder;
import org.hibernate.criterion.Order;

/**
 * Utility that helps map sort API parameters to hibernate directives.
 *
 * @author Michael Krotscheck
 */
public final class SortUtil {

    /**
     * Private constructor for utility classes.
     */
    private SortUtil() {

    }

    /**
     * Build a sort order, based on the enum and the provided field.
     *
     * @param order      The sort order.
     * @param columnName The column name.
     * @return The hibernate sort order.
     */
    public static Order order(final SortOrder order, final String columnName) {
        if (SortOrder.DESC.equals(order)) {
            return Order.desc(columnName);
        }
        return Order.asc(columnName);
    }
}
