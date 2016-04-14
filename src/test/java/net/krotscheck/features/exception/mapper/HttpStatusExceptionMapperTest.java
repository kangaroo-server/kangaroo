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

package net.krotscheck.features.exception.mapper;

import net.krotscheck.features.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.features.exception.exception.HttpForbiddenException;
import net.krotscheck.features.exception.exception.HttpNotFoundException;
import net.krotscheck.features.exception.exception.HttpRedirectException;
import net.krotscheck.features.exception.exception.HttpUnauthorizedException;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Test that jersey exceptions are caught and rewritten into appropriate
 * responses.
 *
 * @author Michael Krotscheck
 */
public class HttpStatusExceptionMapperTest {

    @Test
    public void testForbidden() throws Exception {
        HttpStatusExceptionMapper mapper = new HttpStatusExceptionMapper();
        HttpForbiddenException e = new HttpForbiddenException();

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, er.getHttpStatus());
        Assert.assertEquals("Forbidden", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }

    @Test
    public void testNotFound() throws Exception {
        HttpStatusExceptionMapper mapper = new HttpStatusExceptionMapper();
        HttpNotFoundException e = new HttpNotFoundException();

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, er.getHttpStatus());
        Assert.assertEquals("Not Found", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }

    @Test
    public void testRedirect() throws Exception {
        HttpStatusExceptionMapper mapper = new HttpStatusExceptionMapper();
        HttpRedirectException e =
                new HttpRedirectException("http://example.com/");

        Response r = mapper.toResponse(e);

        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());
        Assert.assertEquals("http://example.com/" +
                "?error=true&http_status=303&error_message=See+Other",
                r.getHeaderString(HttpHeaders.LOCATION));
    }

    @Test
    public void testUnauthorized() throws Exception {
        HttpStatusExceptionMapper mapper = new HttpStatusExceptionMapper();
        HttpUnauthorizedException e = new HttpUnauthorizedException();

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED, er.getHttpStatus());
        Assert.assertEquals("Unauthorized", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }

}