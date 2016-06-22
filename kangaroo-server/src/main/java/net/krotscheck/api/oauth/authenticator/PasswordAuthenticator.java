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

package net.krotscheck.api.oauth.authenticator;

import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidRequestException;
import net.krotscheck.features.database.entity.Authenticator;
import net.krotscheck.features.database.entity.UserIdentity;
import net.krotscheck.kangaroo.common.security.PasswordUtil;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * The PasswordAuthenticator is a specific implementation of the
 * IAuthenticator interface to support the Owner Credentials flow. It is
 * injected into the scope without a name, so that it cannot be chosen using
 * the standard multiauth feature.
 *
 * @author Michael Krotscheck
 */
public final class PasswordAuthenticator
        extends AbstractAuthenticator
        implements IAuthenticator {

    /**
     * Hibernate session, for data access.
     */
    private final Session session;

    /**
     * Create a new password authenticator.
     *
     * @param session An injected hibernate session.
     */
    @Inject
    public PasswordAuthenticator(final Session session) {
        this.session = session;
    }

    /**
     * Do nothing, this authenticator does not delegate.
     *
     * @param configuration The authenticator configuration.
     * @param callback      The redirect, on this server, where the response
     *                      should go.
     * @return An HTTP response, redirecting the client to the next step.
     */
    @Override
    public Response delegate(final Authenticator configuration,
                             final URI callback) {
        return null;
    }

    /**
     * Resolve and/or create a user identity for a specific client, given the
     * returned URI.
     *
     * @param configuration The authenticator configuration.
     * @param parameters    Parameters for the authenticator, retrieved from
     *                      an appropriate source.
     * @return A user identity.
     */
    @Override
    public UserIdentity authenticate(final Authenticator configuration,
                                     final MultivaluedMap<String, String>
                                             parameters) {
        // Validate the input
        if (configuration == null || parameters == null) {
            throw new InvalidRequestException();
        }

        // Extract the login and password.
        String login = getOne(parameters, "username");
        String password = getOne(parameters, "password");

        // Try to find this user.
        Criteria c = session.createCriteria(UserIdentity.class);
        c.add(Restrictions.eq("remoteId", login));
        c.add(Restrictions.eq("authenticator", configuration));
        c.setFirstResult(0);
        c.setMaxResults(1);
        List<UserIdentity> results = c.list();
        if (c.list().size() == 0) {
            return null;
        }

        // Is the password correct?
        UserIdentity identity = results.get(0);
        Boolean isValid = PasswordUtil.isValid(password, identity.getSalt(),
                identity.getPassword());
        if (!isValid) {
            return null;
        }
        return identity;
    }


    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(PasswordAuthenticator.class)
                    .to(PasswordAuthenticator.class)
                    .in(RequestScoped.class);
        }
    }
}
