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

package net.krotscheck.kangaroo.authz.common.authenticator.facebook;

import com.google.common.base.Strings;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.ThirdPartyErrorException;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidRequestException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.ServerErrorException;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * This authentication helper permits using facebook as an IdP. It's not a
 * true facebook app: it immediately discards any issued token, and
 * does no subsequent lookups on the user. In other words, this will not
 * provide you with fancy amazing facebook features, though we may choose to
 * enable this in the future.
 *
 * @author Michael Krotscheck
 */
public final class FacebookAuthenticator implements IAuthenticator {

    /**
     * Map reference, used for decoding.
     */
    public static final GenericType<HashMap<String, String>> MAP_TYPE =
            new GenericType<HashMap<String, String>>() {
            };
    /**
     * The /authorization endpoint.
     */
    private static final String AUTH_ENDPOINT =
            "https://www.facebook.com/v2.10/dialog/oauth";
    /**
     * The /token endpoint.
     */
    private static final String TOKEN_ENDPOINT =
            "https://graph.facebook.com/v2.10/oauth/access_token";
    /**
     * Facebook's base user endpoint.
     */
    private static final String USER_ENDPOINT =
            "https://graph.facebook.com/v2.10/me";
    /**
     * The list of facebook scopes we should request.
     */
    private static final String[] SCOPES = new String[]{"public_profile",
            "email"};
    /**
     * HTTP Client, used for API calls.
     */
    private final Client client;

    /**
     * The database session.
     */
    private final Session session;

    /**
     * Create a new authenticator.
     *
     * @param client  An HTTP Client.
     * @param session The HTTP Session.
     */
    @Inject
    public FacebookAuthenticator(final Client client,
                                 final Session session) {
        this.client = client;
        this.session = session;
    }

    /**
     * This method returns the original URI, without any query or fragment
     * parameters.
     *
     * @param uri The URI to manipulate.
     * @return The previous URI, without any query or fragment.
     */
    private URI buildRedirect(final URI uri) {
        return UriBuilder.fromUri(uri)
                .fragment(null)
                .replaceQuery(null)
                .build();
    }

    /**
     * Delegate an authentication request to facebook.
     *
     * @param authenticator The authenticator configuration.
     * @param callback      The redirect, on this server, where the response
     *                      should go.
     * @return A response indicating the initial request to facebeook.
     */
    @Override
    public Response delegate(final Authenticator authenticator,
                             final URI callback) {
        if (callback == null) {
            throw new ServerErrorException();
        }

        // Extract the state, since OAuth2 requires it be sent separately,
        String state = HttpUtil.parseQueryParams(callback)
                .getFirst("state");
        if (Strings.isNullOrEmpty(state)) {
            throw new ServerErrorException();
        }
        URI redirect = buildRedirect(callback);

        FacebookConfiguration fbConfig =
                FacebookConfiguration.from(authenticator);

        UriBuilder b = UriBuilder
                .fromUri(AUTH_ENDPOINT)
                .queryParam("client_id", fbConfig.getClientId())
                .queryParam("redirect_uri", redirect)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .queryParam("scope", String.join(",", SCOPES));

        return Response.status(Status.FOUND)
                .location(b.build())
                .build();
    }

    /**
     * Validate that the configuration values provided are correct.
     *
     * @param authenticator The authenticator configuration.
     * @throws KangarooException Thrown if we cannot use these values.
     */
    @Override
    public void validate(final Authenticator authenticator)
            throws KangarooException {
        // This will throw if it has problems.
        FacebookConfiguration.from(authenticator);
    }

    /**
     * Handle the response from facebook.
     *
     * @param authenticator The authenticator configuration.
     * @param parameters    Parameters for the authenticator, retrieved from
     *                      an appropriate source.
     * @param callback      The redirect, on this server, where the response
     *                      should go.
     * @return A user identity, or a runtime error that will be sent back.
     */
    @Override
    public UserIdentity authenticate(final Authenticator authenticator,
                                     final MultivaluedMap<String, String>
                                             parameters,
                                     final URI callback) {
        URI redirect = buildRedirect(callback);

        // Is this an error response?
        if (parameters.containsKey("error")) {
            throw new ThirdPartyErrorException(parameters);
        }

        // We're expecting the authorization code.
        String code = parameters.getFirst("code");
        if (Strings.isNullOrEmpty(code)) {
            throw new InvalidRequestException();
        }

        // Retrieve the access token from facebook.
        FacebookConfiguration fbConfig =
                FacebookConfiguration.from(authenticator);
        FacebookIdPToken token = resolveIdPToken(
                fbConfig.getClientId(), fbConfig.getClientSecret(),
                code, redirect);
        if (Strings.isNullOrEmpty(token.getAccessToken())) {
            throw new ThirdPartyErrorException();
        }

        FacebookUserEntity fbUser = loadUserIdentity(token);
        if (Strings.isNullOrEmpty(fbUser.getId())) {
            throw new ThirdPartyErrorException();
        }

        // Try to find an identity.
        Application application = authenticator.getClient().getApplication();
        Criteria criteria = session.createCriteria(UserIdentity.class)
                .createAlias("user", "u")
                .add(Restrictions.eq("type", AuthenticatorType.Facebook))
                .add(Restrictions.eq("remoteId", fbUser.getId()))
                .add(Restrictions.eq("u.application", application))
                .setFirstResult(0)
                .setMaxResults(1);
        UserIdentity identity = (UserIdentity) criteria.uniqueResult();

        // See if this is a brand new user.
        if (identity == null) {
            User u = new User();
            u.setApplication(application);
            u.setRole(application.getDefaultRole());

            identity = new UserIdentity();
            identity.setUser(u);
            identity.setType(AuthenticatorType.Facebook);
            identity.setRemoteId(fbUser.getId());
            identity.setClaims(fbUser.toClaims());

            session.save(u);
            session.save(identity);
        } else {
            identity.getClaims().putAll(fbUser.toClaims());
            session.save(identity);
        }

        // Query for the user id in this application.
        return identity;
    }

    /**
     * Load the user's identity from facebook, and match it against our
     * internal database.
     *
     * @param token The facebook token.
     * @return T
     */
    private FacebookUserEntity loadUserIdentity(final FacebookIdPToken token) {
        Response r = client.target(USER_ENDPOINT)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getAccessToken()))
                .get();

        try {
            // If this is an error...
            if (r.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                return r.readEntity(FacebookUserEntity.class);
            } else {
                Map<String, String> params = r.readEntity(MAP_TYPE);
                throw new ThirdPartyErrorException(params);
            }
        } catch (ProcessingException e) {
            throw new ThirdPartyErrorException();
        } finally {
            r.close();
        }
    }

    /**
     * Resolve an authorization code to an oauth token.
     *
     * @param clientId          The client id.
     * @param clientSecret      The client secret.
     * @param authorizationCode The authorization code.
     * @param redirectUrl       The redirect URL.
     * @return An OAuth2 token entity.
     */
    private FacebookIdPToken resolveIdPToken(final String clientId,
                                             final String clientSecret,
                                             final String authorizationCode,
                                             final URI redirectUrl) {
        String authHeader = HttpUtil.authHeaderBasic(clientId, clientSecret);

        // Build the request payload
        Form f = new Form();
        f.param("client_id", clientId);
        f.param("code", authorizationCode);
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", redirectUrl.toString());
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        // Issue the request
        Response r = client.target(TOKEN_ENDPOINT)
                .request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(postEntity);
        try {
            // If this is an error...
            if (r.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                return r.readEntity(FacebookIdPToken.class);
            } else {
                Map<String, String> params = r.readEntity(MAP_TYPE);
                throw new ThirdPartyErrorException(params);
            }
        } catch (ProcessingException e) {
            throw new ThirdPartyErrorException();
        } finally {
            r.close();
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(FacebookAuthenticator.class)
                    .to(IAuthenticator.class)
                    .named(AuthenticatorType.Facebook.name())
                    .in(RequestScoped.class);
        }
    }
}
