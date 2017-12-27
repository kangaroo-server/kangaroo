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

import java.util.Optional;

/**
 * Simple object manipulation utilities, not found elsewhere.
 *
 * @author Michael Krotscheck
 */
public final class ObjectUtil {

    /**
     * Private constructor - utility class.
     */
    private ObjectUtil() {

    }

    /**
     * A safe-casting idiom for Java8.
     *
     * @param candidate   The instance candidate.
     * @param targetClass The target class.
     * @param <S>         The originating type.
     * @param <T>         The target class.
     * @return An Optional instance of the cast type.
     */
    public static <S, T> Optional<T> safeCast(final S candidate,
                                              final Class<T> targetClass) {
        return targetClass.isInstance(candidate)
                ? Optional.of(targetClass.cast(candidate))
                : Optional.empty();
    }
}
