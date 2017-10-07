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

package net.krotscheck.kangaroo.authz.oauth2.session.grizzly;

import net.krotscheck.kangaroo.authz.AuthzServerConfig;
import net.krotscheck.kangaroo.authz.common.database.entity.HttpSession;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.common.config.SystemConfiguration;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Session;
import org.glassfish.grizzly.http.server.SessionManager;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * The grizzly session manager overrides the SessionManager from the grizzly
 * servlet container with itself, in order to provide a manager that is bound
 * to the Jersey2 injection context. The purpose of this is to be able to
 * access the same injectable resources - such as a database connection, when
 * resolving session persistence.
 *
 * @author Michael Krotscheck
 */
public final class GrizzlySessionManager implements SessionManager {

    /**
     * Max age, in seconds, of the session.
     */
    private final Integer sessionMaxAge;

    /**
     * The context sensitive session factory provider.
     */
    private final Provider<SessionFactory> sessionFactoryProvider;

    /**
     * The name of the session (aka cookie).
     */
    private String sessionCookieName;

    /**
     * Create a new instance of this session manager.
     *
     * @param c          The system configuration.
     * @param sfProvider The session factory provider.
     */
    @Inject
    public GrizzlySessionManager(final SystemConfiguration c,
                                 final Provider<SessionFactory> sfProvider) {
        this.sessionFactoryProvider = sfProvider;

        // Get the session name and expiry.
        sessionMaxAge = c.getInt(
                AuthzServerConfig.SESSION_MAX_AGE.getKey(),
                AuthzServerConfig.SESSION_MAX_AGE.getValue());
        sessionCookieName = c.getString(
                AuthzServerConfig.SESSION_NAME.getKey(),
                AuthzServerConfig.SESSION_NAME.getValue());
    }

    /**
     * Return the session associated with this Request.
     *
     * @param request            {@link Request}
     * @param requestedSessionId the session id associated with the
     *                           {@link Request}
     * @return {@link Session}
     */
    @Override
    public Session getSession(final Request request,
                              final String requestedSessionId) {
        BigInteger sessionId = IdUtil.fromString(requestedSessionId);

        if (sessionId != null) {
            SessionFactory sessionFactory = sessionFactoryProvider.get();
            org.hibernate.Session hibernateSession =
                    sessionFactory.openSession();

            hibernateSession.beginTransaction();
            HttpSession entity =
                    hibernateSession.get(HttpSession.class, sessionId);
            hibernateSession.getTransaction().commit();

            if (entity != null) {
                // quick persist to update modified date.
                Calendar now =
                        Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                entity.setModifiedDate(now);

                hibernateSession.beginTransaction();
                hibernateSession.update(entity);
                hibernateSession.getTransaction().commit();
            }
            hibernateSession.close();

            return asSession(entity);
        }

        return null;
    }

    /**
     * Create a new {@link Session} associated with the {@link Request}.
     *
     * @param request {@link Request}
     * @return a new {@link Session} associated with the {@link Request}
     */
    @Override
    public Session createSession(final Request request) {
        SessionFactory sessionFactory = sessionFactoryProvider.get();
        org.hibernate.Session hibernateSession = sessionFactory.openSession();

        hibernateSession.beginTransaction();
        HttpSession entity = new HttpSession();
        entity.setSessionTimeout(sessionMaxAge);
        hibernateSession.save(entity);
        hibernateSession.getTransaction().commit();
        hibernateSession.close();

        return asSession(entity);
    }

    /**
     * Change the session ID of a provided session in-place.
     *
     * @param request The request context.
     * @param session The session whose ID to change.
     * @return The old session ID.
     */
    @Override
    public String changeSessionId(final Request request,
                                  final Session session) {
        String oldId = session.getIdInternal();
        GrizzlySession grizzlySession = (GrizzlySession) session;
        SessionFactory sessionFactory = sessionFactoryProvider.get();
        org.hibernate.Session hibernateSession = sessionFactory.openSession();

        try {
            BigInteger old = IdUtil.fromString(session.getIdInternal());

            hibernateSession.beginTransaction();
            HttpSession oldEntity =
                    hibernateSession.get(HttpSession.class, old);
            hibernateSession.getTransaction().commit();

            if (oldEntity != null) {
                hibernateSession.beginTransaction();
                List<OAuthToken> refreshTokens =
                        new ArrayList<>(oldEntity.getRefreshTokens());
                HttpSession newSession = new HttpSession();
                newSession.setSessionTimeout(session.getSessionTimeout());
                newSession.setRefreshTokens(refreshTokens);

                hibernateSession.delete(oldEntity);
                hibernateSession.save(newSession);

                hibernateSession.getTransaction().commit();

                grizzlySession
                        .setIdInternal(IdUtil.toString(newSession.getId()));
            }
        } catch (Exception e) {
            hibernateSession.getTransaction().rollback();
        }

        hibernateSession.close();

        return oldId;
    }


    /**
     * Return the current session cookie name.
     *
     * @return The current cookie name.
     */
    @Override
    public String getSessionCookieName() {
        return sessionCookieName;
    }

    /**
     * Set the current session cookie name.
     *
     * @param name The name of the cookie.
     */
    @Override
    public void setSessionCookieName(final String name) {
        this.sessionCookieName = name;
    }

    /**
     * Configure session cookie before adding it to the
     * {@link Request#getResponse()}.
     *
     * @param request The request.
     * @param cookie  The cookie to configure.
     */
    @Override
    public void configureSessionCookie(final Request request,
                                       final Cookie cookie) {
        String requestUrlString = request.getRequestURL().toString();
        URI requestUrl = URI.create(requestUrlString);

        // Most of these properties are overridden by the session container
        // after this has been set, but we write them here just in case
        // there's a refactor in the future that breaks it.
        cookie.setDomain(requestUrl.getHost());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setName(sessionCookieName);
        cookie.setMaxAge(sessionMaxAge);
        cookie.setVersion(1);
    }

    /**
     * Convert a database entity session to a regular detached grizzly session.
     *
     * @param entity The entity to convert.
     * @return The HTTP session.
     */
    protected Session asSession(final HttpSession entity) {
        if (entity == null) {
            return null;
        }

        GrizzlySession s = new GrizzlySession();
        s.setIdInternal(IdUtil.toString(entity.getId()));
        s.setCreationTime(entity.getCreatedDate().getTimeInMillis());
        s.setTimestamp(entity.getModifiedDate().getTimeInMillis());
        s.setSessionTimeout(entity.getSessionTimeout());
        return s;
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(GrizzlySessionManager.class)
                    .to(GrizzlySessionManager.class)
                    .in(Singleton.class);
        }
    }
}
