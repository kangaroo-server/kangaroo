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

package net.krotscheck.kangaroo.authz.oauth2.session;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import java.util.function.Supplier;

/**
 * A quick factory that extracts the HTTPSession on request.
 *
 * @author Michael Krotscheck
 */
public final class HttpSessionFactory implements Supplier<HttpSession> {

    /**
     * The request provider.
     */
    private final Provider<HttpServletRequest> requestProvider;

    /**
     * Create a new instance of this factory.
     *
     * @param requestProvider The request provider.
     */
    public HttpSessionFactory(
            @Context final Provider<HttpServletRequest> requestProvider) {
        this.requestProvider = requestProvider;
    }

    /**
     * Extract, and provide, the HTTP Session from the servlet request.
     *
     * @return The HTTP Session.
     */
    @Override
    public HttpSession get() {
        return requestProvider.get().getSession();
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(HttpSessionFactory.class)
                    .to(HttpSession.class)
                    .in(RequestScoped.class);
        }
    }
}
