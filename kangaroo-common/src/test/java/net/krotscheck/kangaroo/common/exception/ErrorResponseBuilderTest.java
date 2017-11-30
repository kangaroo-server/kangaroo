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

package net.krotscheck.kangaroo.common.exception;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.exception.KangarooExceptionTest.TestError;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.mockito.Mockito.mock;

/**
 * Test the various ways we can generate an application exception.
 *
 * @author Michael Krotscheck
 */
public final class ErrorResponseBuilderTest {

    /**
     * Test building an error response from a status code.
     */
    @Test
    public void testFromHttpStatusCode() {
        Response r = ErrorResponseBuilder.from(
                Status.NOT_FOUND).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.NOT_FOUND, er.getHttpStatus());
        Assert.assertEquals("Not Found", er.getErrorDescription());
        Assert.assertEquals("not_found", er.getError());
    }

    /**
     * Test building an error from a status code and a message.
     */
    @Test
    public void testFromStatusAndMessage() {
        Response r = ErrorResponseBuilder.from(
                Status.NOT_FOUND,
                "message").build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.NOT_FOUND, er.getHttpStatus());
        Assert.assertEquals("message", er.getErrorDescription());
        Assert.assertEquals("not_found", er.getError());
    }

    /**
     * Test building an error from a status code, message, and error code.
     */
    @Test
    public void testFromStatusCodeAndMessage() {
        Response r = ErrorResponseBuilder.from(
                Status.NOT_FOUND,
                "message", "test_code").build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.NOT_FOUND, er.getHttpStatus());
        Assert.assertEquals("message", er.getErrorDescription());
        Assert.assertEquals("test_code", er.getError());
    }

    /**
     * Test building a JSONParseException.
     */
    @Test
    public void testFromJsonParseException() {
        JsonParseException e = new JsonParseException("foo",
                mock(JsonLocation.class));

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.BAD_REQUEST, er.getHttpStatus());
        Assert.assertTrue(er.getErrorDescription().indexOf("foo") > -1);
        Assert.assertEquals("bad_request", er.getError());
    }

    /**
     * Test building from a KangarooException.
     */
    @Test
    public void testFromKangarooException() {
        KangarooException e = new TestError();

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.BAD_REQUEST, er.getHttpStatus());
        Assert.assertEquals("Test Error",
                er.getErrorDescription());
        Assert.assertEquals("test_error", er.getError());
    }

    /**
     * Test building from a WebApplicationException.
     */
    @Test
    public void testFromWebApplicationException() {
        WebApplicationException e = new WebApplicationException();

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, er.getHttpStatus());
        Assert.assertEquals("HTTP 500 Internal Server Error",
                er.getErrorDescription());
        Assert.assertEquals("internal_server_error", er.getError());
    }

    /**
     * Test building from a ConstraintViolationException.
     */
    @Test
    public void testFromConstraintException() {
        Set<ConstraintViolation<Application>> violations = new HashSet<>();
        ConstraintViolation m = Mockito.mock(ConstraintViolation.class);
        Mockito.when(m.getMessage()).thenReturn("test 1");
        violations.add(m);

        ConstraintViolationException e =
                new ConstraintViolationException("message", violations);

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.BAD_REQUEST, er.getHttpStatus());
        Assert.assertEquals("test 1", er.getErrorDescription());
        Assert.assertEquals("bad_request", er.getError());
    }

    /**
     * Test building from a ConstraintViolationException with no violations.
     */
    @Test
    public void testFromConstraintExceptionNoViolations() {
        Set<ConstraintViolation<Application>> violations = new HashSet<>();
        ConstraintViolationException e =
                new ConstraintViolationException("message", violations);

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, er.getHttpStatus());
        Assert.assertEquals("Internal Server Error", er.getErrorDescription());
        Assert.assertEquals("internal_server_error", er.getError());
    }

    /**
     * Test building from a generic exception.
     */
    @Test
    public void testFromException() {
        Exception e = new Exception();

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, er.getHttpStatus());
        Assert.assertEquals("Internal Server Error", er.getErrorDescription());
        Assert.assertEquals("internal_server_error", er.getError());
    }

    /**
     * Test building only an entity.
     */
    @Test
    public void testEntityOnly() {
        Exception e = new Exception();

        ErrorResponse r = ErrorResponseBuilder.from(e).buildEntity();

        Assert.assertEquals("Internal Server Error", r.getErrorDescription());
        Assert.assertEquals("internal_server_error", r.getError());
    }

    /**
     * Test building from a generic exception.
     */
    @Test
    public void testWithHeader() {
        Exception e = new Exception();

        Response r = ErrorResponseBuilder.from(e)
                .addHeader("test", "test")
                .build();
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals("test", r.getHeaderString("test"));
    }

    /**
     * Test serializing to json.
     *
     * @throws Exception Should not be thrown (hopefully).
     */
    @Test
    public void testSerialization() throws Exception {
        Exception e = new Exception();

        Response r = ErrorResponseBuilder.from(e).build();
        ErrorResponse er = (ErrorResponse) r.getEntity();
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(er);
        JsonNode node = mapper.readTree(jsonString);

        // Count the field names, we're expecting two.
        int fieldCount = 0;
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            nameIterator.next();
            fieldCount++;
        }
        Assert.assertEquals(2, fieldCount);

        Assert.assertEquals("Internal Server Error",
                node.get("error_description").asText());
        Assert.assertEquals("internal_server_error",
                node.get("error").asText());
    }
}
