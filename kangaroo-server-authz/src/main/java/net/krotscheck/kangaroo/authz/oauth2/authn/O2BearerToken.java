/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.oauth2.authn;

import javax.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation enables authentication via an already authorized bearer
 * token.
 *
 * @author Michael Krotscheck
 */
@Target({ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@NameBinding
public @interface O2BearerToken {

    /**
     * Whether to permit private clients.
     *
     * @return True if private clients are permitted, otherwise false.
     */
    boolean permitPrivate() default true;

    /**
     * Whether to permit public clients.
     *
     * @return True if public clients are permitted, otherwise false.
     */
    boolean permitPublic() default true;
}
