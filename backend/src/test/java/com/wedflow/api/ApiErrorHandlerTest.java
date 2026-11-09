package com.wedflow.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class ApiErrorHandlerTest {
 private final ApiErrorHandler handler=new ApiErrorHandler();
 @Test void businessConflictPreservesStatusAndReference(){
  var req=new MockHttpServletRequest();req.setAttribute("requestId","trace-1");
  var result=handler.status(new ResponseStatusException(HttpStatus.CONFLICT,"Cannot delete wedding with bookings"),req);
  assertEquals(409,result.getStatusCode().value());
  assertEquals("trace-1",result.getBody().getProperties().get("requestId"));
 }
 @Test void serverExceptionDoesNotExposeDetails(){
  var req=new MockHttpServletRequest();req.setAttribute("requestId","trace-2");
  var result=handler.unexpected(new IllegalStateException("database password secret"),req);
  assertEquals(500,result.getStatusCode().value());
  assertFalse(result.getBody().getDetail().contains("secret"));
 }
}
