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

package net.krotscheck.kangaroo.util;

import com.google.common.base.Strings;

/**
 * Simple string utilities not covered elsewhere.
 *
 * @author Michael Krotscheck
 */
public final class StringUtil {

    /**
     * Private constructor - utility class.
     */
    private StringUtil() {

    }

    /**
     * Return the string, or a default value if the string is null/empty.
     *
     * @param input The input string.
     * @param def   The default to return.
     * @return The provided string, or the default if it is empty.
     */
    public static String sameOrDefault(final String input,
                                       final String def) {
        if (Strings.isNullOrEmpty(input)) {
            return def;
        }
        return input;
    }
}
