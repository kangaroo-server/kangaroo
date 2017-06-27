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

package net.krotscheck.kangaroo.authz.admin.v1.auth.exception;

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import net.krotscheck.kangaroo.common.exception.KangarooException.ErrorCode;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Error mapper for all WWW-Authorization challenge exceptions. Ensures that
 * the WWW-Authenticate header is added to the response.
 *
 * @author Michael Krotscheck
 */
public final class WWWChallengeExceptionMapper
        implements ExceptionMapper<WWWChallengeException> {

    /**
     * Convert to response.
     *
     * @param exception The exception to convert.
     * @return A Response instance for this error.
     */
    public Response toResponse(final WWWChallengeException exception) {
        String authHeader = buildAuthHeader(exception);
        return ErrorResponseBuilder.from(exception)
                .addHeader(HttpHeaders.WWW_AUTHENTICATE, authHeader)
                .build();
    }

    /**
     * This method adds a WWW-Authenticate header to the response wrapped
     * in this exception.
     *
     * @param exception The exception from which to read our required
     *                  values.
     * @return The raw bearer challenge for the WWW-Authenticate header.
     */
    private String buildAuthHeader(final WWWChallengeException exception) {
        ErrorCode code = exception.getCode();

        String realm = exception.getRealm().toString();
        String scopes = String.join(" ", exception.getRequiredScopes());
        String error = code.getError();
        String description = code.getErrorDescription();

        List<String> entries = new ArrayList<>();
        entries.add(String.format("realm=\"%s\"", realm));
        entries.add(String.format("scope=\"%s\"", scopes));
        entries.add(String.format("error=\"%s\"", error));
        entries.add(String.format("error_description=\"%s\"", description));

        return String.format("Bearer %s", String.join(", ", entries));
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(WWWChallengeExceptionMapper.class)
                    .to(ExceptionMapper.class)
                    .in(Singleton.class);
        }
    }
}
