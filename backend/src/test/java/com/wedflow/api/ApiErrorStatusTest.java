package com.wedflow.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import static org.junit.jupiter.api.Assertions.*;

class ApiErrorStatusTest {
 private final ApiErrorHandler handler=new ApiErrorHandler();
 private MockHttpServletRequest request(){
  var req=new MockHttpServletRequest();
  req.setAttribute("requestId","test-request-id");
  return req;
 }
 @Test void missingRequiredQueryParameterIsClientError() throws Exception {
  var result=handler.malformed(new MissingServletRequestParameterException("status","String"),request());
  assertEquals(400,result.getStatusCode().value());
 }
 @Test void unsupportedMethodIsNotInternalFailure() {
  var result=handler.method(new HttpRequestMethodNotSupportedException("TRACE"),request());
  assertEquals(405,result.getStatusCode().value());
 }
 @Test void accessDeniedIsForbidden() {
  var result=handler.forbidden(new AccessDeniedException("sensitive detail"),request());
  assertEquals(HttpStatus.FORBIDDEN,result.getStatusCode());
  assertFalse(result.getBody().getDetail().contains("sensitive"));
 }
}
