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

package net.krotscheck.kangaroo.authz.admin.v1.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the list of scopes permitted to access method(s) in an application.
 * The value of the ScopesAllowed annotation is a list of scope names.
 * This annotation can be specified on a class or on method(s). Specifying it
 * at a class level means that it applies to all the methods in the class.
 * Specifying it on a method means that it is applicable to that method only.
 * If applied at both the class and methods level , the method value overrides
 * the class value if the two conflict.
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface ScopesAllowed {
    /**
     * List of scopes that are permitted for this resource.
     *
     * @return The list of scopes.
     */
    String[] value();
}
