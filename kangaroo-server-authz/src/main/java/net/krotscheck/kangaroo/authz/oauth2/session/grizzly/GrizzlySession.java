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

import org.glassfish.grizzly.http.server.Session;

/**
 * This extends the default session implementation, as the one provided by
 * grizzly (and required by it) doesn't actually do all the things we need it
 * to.
 *
 * @author Michael Krotscheck
 */
public final class GrizzlySession extends Session {

    /**
     * When this session was created.
     */
    private long creationTime = System.currentTimeMillis();

    /**
     * A session identifier.
     */
    private String id = null;

    /**
     * Is this Session valid.
     */
    private boolean isValid = true;

    /**
     * Is this session new.
     */
    private boolean isNew = true;

    /**
     * Creation time stamp.
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * @return the session identifier for this session.
     */
    @Override
    public String getIdInternal() {
        return id;
    }

    /**
     * Sets the session identifier for this session.
     *
     * @param id The internal ID.
     */
    @Override
    public void setIdInternal(final String id) {
        this.id = id;
    }

    /**
     * Is the current Session valid?
     *
     * @return true if valid.
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Set this object as validated.
     *
     * @param isValid Set whether this session is valid.
     */
    public void setValid(final boolean isValid) {
        this.isValid = isValid;

        if (!isValid) {
            timestamp = -1;
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
    public boolean isNew() {
        return isNew;
    }

    /**
     * Returns the time when this session was created, measured
     * in milliseconds since midnight January 1, 1970 GMT.
     *
     * @return a <code>long</code> specifying when this session was created,
     * expressed in milliseconds since 1/1/1970 GMT
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Set the creation time of this session.
     *
     * @param creationTime The creation time, in milliseconds.
     */
    public void setCreationTime(final long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * @return the timestamp when this session was accessed the last time
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp when this session was accessed the last time.
     *
     * @param timestamp A long representing when the session was last accessed.
     */
    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Updates the "last accessed" timestamp with the current time.
     *
     * @return the time stamp
     */
    public long access() {
        final long localTimeStamp = System.currentTimeMillis();
        timestamp = localTimeStamp;
        isNew = false;

        return localTimeStamp;
    }
}
