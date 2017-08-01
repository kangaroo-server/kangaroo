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

package net.krotscheck.kangaroo.authz.common.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.NotImplementedException;
import org.glassfish.grizzly.http.server.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * This hibernate entity describes an HTTP session that maintains 'refresh
 * token' references on behalf of the user. It's used to permit renewing a
 * user's token, without forcing them to log in multiple times.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "http_sessions")
public final class HttpSession extends Session {

    /**
     * A 64-byte id that identifies this particular session.
     */
    @Id
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type"
            + ".BigIntegerType")
    @GenericGenerator(name = "secure_random_bytes",
            parameters = @Parameter(name = "byteCount", value = "64"),
            strategy = "net.krotscheck.kangaroo.authz.common.database.util"
                    + ".SecureRandomIdGenerator")
    @GeneratedValue(generator = "secure_random_bytes")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private BigInteger id = null;

    /**
     * OAuth tokens attached to this session.
     */
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "httpSession",
            cascade = {CascadeType.REMOVE, CascadeType.MERGE},
            orphanRemoval = true
    )
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<OAuthToken> refreshTokens = new ArrayList<>();

    /**
     * The date this record was created.
     */
    @Column(name = "creationTime")
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type"
            + ".CalendarTimestampType")
    private Calendar creationTime =
            Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * Is this session new.
     */
    @Transient
    private boolean isNew = false;

    /**
     * Timeout.
     */
    @Basic(optional = false)
    @Column(name = "sessionTimeout", nullable = false)
    private long sessionTimeout = -1;

    /**
     * Last accessed timestamp.
     */
    @Column(name = "timestamp")
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type"
            + ".CalendarTimestampType")
    private Calendar timestamp =
            Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * Create a new session instance.
     */
    public HttpSession() {
        super();
    }

    /**
     * Is the current Session valid?
     *
     * @return true if valid.
     */
    @Override
    @Transient
    @JsonIgnore
    public boolean isValid() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar expireDate = (Calendar) timestamp.clone();
        expireDate.add(Calendar.SECOND,
                ((Long) getSessionTimeout()).intValue());
        return now.before(expireDate);
    }

    /**
     * Set this object as validated.
     *
     * @param isValid
     */
    @Override
    public void setValid(final boolean isValid) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (!isValid) {
            now.add(Calendar.SECOND, -((Long) getSessionTimeout()).intValue());
            setTimestamp(now.getTimeInMillis());
        } else {
            this.setTimestamp(now.getTimeInMillis());
        }
    }

    /**
     * Returns <code>true</code> if the client does not yet know about the
     * session or if the client chooses not to join the session.  For
     * example, if the server used only cookie-based sessions, and
     * the client had disabled the use of cookies, then a session would
     * be new on each request.
     *
     * @return <code>true</code> if the
     * server has created a session,
     * but the client has not yet joined
     */
    @Override
    public boolean isNew() {
        return isNew;
    }

    /**
     * Set whether the browser already knows about this session.
     *
     * @param isNew Whether the session is new.
     */
    public void setNew(final boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * Add an attribute to this session. This is a no-op, as the system only
     * permits attaching refresh tokens to the session.
     *
     * @param key   The key.
     * @param value The value.
     */
    @Override
    public void setAttribute(final String key, final Object value) {
        throw new NotImplementedException("Please use setRefreshTokens");
    }

    /**
     * Return an attribute. This is a no-op, please access the collection
     * directly.
     *
     * @param key The key to check.
     * @return Always null.
     */
    @Override
    public Object getAttribute(final String key) {
        throw new NotImplementedException("Please use getRefreshTokens");
    }

    /**
     * Remove an attribute. This is a no-op, please access the collection
     * directly.
     *
     * @param key The key to remove.
     * @return Always false.
     */
    @Override
    public Object removeAttribute(final String key) {
        throw new NotImplementedException("Please use getRefreshTokens");
    }

    /**
     * Returns the time when this session was created, measured
     * in milliseconds since midnight January 1, 1970 GMT.
     *
     * @return a <code>long</code> specifying when this session was created,
     * expressed in  milliseconds since 1/1/1970 GMT
     */
    @Override
    public long getCreationTime() {
        return creationTime.getTimeInMillis();
    }

    /**
     * Set the creation time for this session.
     *
     * @param creationTime The new creation time, expressed in milliseconds GMT.
     */
    public void setCreationTime(final long creationTime) {
        this.creationTime.setTimeInMillis(creationTime);
    }

    /**
     * Return a long representing the maximum idle time (in milliseconds) a
     * session can be.
     *
     * @return a long representing the maximum idle time (in milliseconds) a
     * session can be.
     */
    @Override
    public long getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Set a long representing the maximum idle time (in milliseconds) a
     * session can be.
     *
     * @param sessionTimeout a long representing the maximum idle time
     *                       (in milliseconds) a session can be.
     */
    @Override
    public void setSessionTimeout(final long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * @return the timestamp when this session was accessed the last time
     */
    @Override
    public long getTimestamp() {
        return timestamp.getTimeInMillis();
    }

    /**
     * Set the timestamp when this session was accessed the last time.
     *
     * @param timestamp a long representing when the session was accessed the
     *                  last time
     */
    @Override
    public void setTimestamp(final long timestamp) {
        this.timestamp.setTimeInMillis(timestamp);
    }

    /**
     * Return a {@link List} of refreshTokens.
     *
     * @return the refresh tokens associated with this session.
     */
    public List<OAuthToken> getRefreshTokens() {
        return refreshTokens;
    }

    /**
     * Set the list of tokens.
     *
     * @param refreshTokens The refresh tokens attached to this session.
     */
    public void setRefreshTokens(final List<OAuthToken> refreshTokens) {
        this.refreshTokens = new ArrayList<>(refreshTokens);
    }

    /**
     * Set the ID.
     *
     * @return The ID for this session.
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Get the ID.
     *
     * @param id The ID for this session.
     */
    public void setId(final BigInteger id) {
        this.id = id;
    }

    /**
     * @return the session identifier for this session.
     */
    @Override
    @Transient
    public String getIdInternal() {
        return id.toString(16);
    }

    /**
     * Updates the "last accessed" timestamp with the current time.
     *
     * @return the time stamp
     */
    @Override
    @Transient
    public long access() {
        final long localTimeStamp = System.currentTimeMillis();
        timestamp.setTimeInMillis(localTimeStamp);
        isNew = false;
        return localTimeStamp;
    }
}
