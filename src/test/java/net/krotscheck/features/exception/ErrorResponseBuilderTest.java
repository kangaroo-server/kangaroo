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

package net.krotscheck.features.exception;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import net.krotscheck.features.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.features.exception.exception.HttpStatusException;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.mock;

/**
 * Test the various ways we can generate an application exception.
 *
 * @author Michael Krotscheck
 */
public class ErrorResponseBuilderTest {

    @Test
    public void testFromHttpStatusCode() throws Exception {
        Response r = ErrorResponseBuilder.from(
                HttpStatus.SC_NOT_FOUND).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, er.getHttpStatus());
        Assert.assertEquals("Not Found", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }

    @Test
    public void testFromStatusAndMessage() throws Exception {
        Response r = ErrorResponseBuilder.from(
                HttpStatus.SC_NOT_FOUND,
                "message").build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, er.getHttpStatus());
        Assert.assertEquals("message", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }

    @Test
    public void testFromStatusMessageRedirect() throws Exception {
        Response r = ErrorResponseBuilder.from(
                HttpStatus.SC_NOT_FOUND,
                "message",
                "http://example.com/").build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, r.getStatus());
        Assert.assertEquals("http://example.com/" +
                "?error=true&http_status=404&error_message=message",
                r.getHeaderString(HttpHeaders.LOCATION));
    }

    @Test
    public void testFromJsonParseException() throws Exception {
        JsonParseException e = new JsonParseException("foo",
                mock(JsonLocation.class));

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, er.getHttpStatus());
        Assert.assertTrue(er.getErrorMessage().indexOf("foo") > -1);
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }

    @Test
    public void testFromHttpStatusException() throws Exception {
        HttpStatusException e =
                new HttpStatusException(HttpStatus.SC_NOT_FOUND, "foo");

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, er.getHttpStatus());
        Assert.assertEquals("foo", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }

    @Test
    public void testFromWebApplicationException() throws Exception {
        WebApplicationException e = new WebApplicationException();

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, er.getHttpStatus());
        Assert.assertEquals("Internal Server Error", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }

    @Test
    public void testFromException() throws Exception {
        Exception e = new Exception();

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, er.getHttpStatus());
        Assert.assertEquals("Internal Server Error", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
        Assert.assertEquals(true, er.isError());
    }
}