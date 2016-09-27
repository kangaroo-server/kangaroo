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

package net.krotscheck.kangaroo.test;

import net.krotscheck.kangaroo.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import org.apache.http.HttpStatus;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * The dev authenticator provides a simple authenticator implementation which
 * may be used when building a new application and debugging the
 * authentication flow. It only ever creates a single user, "Pat Developer",
 * and always presumes a successful third-party authentication.
 *
 * @author Michael Krotscheck
 */
public final class TestAuthenticator
        implements IAuthenticator {

    /**
     * Unique foreign ID string for the debug user.
     */
    public static final String REMOTE_ID = "dev_user";

    /**
     * Hibernate session, to use for database access.
     */
    private final Session session;

    /**
     * Create a new dev authenticator.
     *
     * @param session Injected hibernate session.
     */
    @Inject
    public TestAuthenticator(final Session session) {
        this.session = session;
    }

    /**
     * Execute an authentication process for a specific request.
     *
     * @param configuration The authenticator configuration.
     * @param callback      The redirect, on this server, where the response
     *                      should go.
     * @return An HTTP response, redirecting the client to the next step.
     */
    @Override
    public Response delegate(final Authenticator configuration,
                             final URI callback) {
        return Response
                .status(HttpStatus.SC_MOVED_TEMPORARILY)
                .location(callback)
                .build();
    }

    /**
     * Resolve and/or create a user identity, given an intermediate state and
     * request parameters.
     *
     * @param authenticator The authenticator configuration.
     * @param parameters    Parameters for the authenticator, retrieved from
     *                      an appropriate source.
     */
    @Override
    public UserIdentity authenticate(final Authenticator authenticator,
                                     final MultivaluedMap<String, String>
                                             parameters) {

        Criteria searchCriteria = session.createCriteria(UserIdentity.class);

        searchCriteria.add(Restrictions.eq("authenticator", authenticator));
        searchCriteria.add(Restrictions.eq("remoteId", REMOTE_ID));
        searchCriteria.setFirstResult(0);
        searchCriteria.setMaxResults(1);

        List<UserIdentity> results = searchCriteria.list();

        // Do we need to create a new user?
        if (results.size() == 0) {
            User devUser = new User();
            devUser.setApplication(authenticator.getClient().getApplication());

            UserIdentity identity = new UserIdentity();
            identity.setAuthenticator(authenticator);
            identity.setRemoteId(REMOTE_ID);
            identity.setUser(devUser);

            Transaction t = session.beginTransaction();
            session.save(devUser);
            session.save(identity);
            t.commit();

            return identity;
        }

        return results.get(0);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(TestAuthenticator.class)
                    .to(IAuthenticator.class)
                    .named("test")
                    .in(RequestScoped.class);
        }
    }
}
