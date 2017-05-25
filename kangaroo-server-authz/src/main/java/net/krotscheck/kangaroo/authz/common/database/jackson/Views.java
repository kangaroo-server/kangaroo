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
 *
 */

package net.krotscheck.kangaroo.authz.common.database.jackson;

/**
 * These views describe classes used in Jackson's view annotations.
 *
 * @author Michael Krotscheck
 */
public final class Views {

    /**
     * A view used for attributes that are considered secure - such as salts,
     * passwords, etc.
     */
    public static final class Secure {
    }

    /**
     * A view used for attributes that are considered public.
     */
    public static final class Public {
    }
}
