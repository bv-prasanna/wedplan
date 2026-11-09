package com.wedflow.audit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ApiAuditInterceptorTest {
 @Test void validClientCoordinatesCarryUnverifiedProvenance(){
  MockHttpServletRequest req=new MockHttpServletRequest();
  req.addHeader("X-Geo-Latitude","12.9716000");
  req.addHeader("X-Geo-Longitude","77.5946000");
  req.addHeader("X-Geo-Accuracy-M","15.5");
  EventLocation location=ApiAuditInterceptor.clientLocation(req);
  assertEquals("CLIENT_REPORTED",location.source());
  assertNotNull(location.latitude());
  assertNotNull(location.recordedAt());
 }
 @Test void missingCoordinatesDoNotDiscardAudit(){
  EventLocation location=ApiAuditInterceptor.clientLocation(new MockHttpServletRequest());
  assertEquals("UNAVAILABLE",location.source());
  assertEquals("NOT_PROVIDED",location.unavailableReason());
 }
 @Test void incompleteOrInvalidCoordinatesAreNotTrusted(){
  MockHttpServletRequest req=new MockHttpServletRequest();
  req.addHeader("X-Geo-Latitude","91");
  req.addHeader("X-Geo-Longitude","77");
  assertEquals("INVALID_COORDINATES",ApiAuditInterceptor.clientLocation(req).unavailableReason());
  MockHttpServletRequest req2=new MockHttpServletRequest();
  req2.addHeader("X-Geo-Latitude","12");
  assertEquals("INVALID_COORDINATES",ApiAuditInterceptor.clientLocation(req2).unavailableReason());
 }
 @Test void extractsOnlyUuidSubjectFromRouteVariables(){
  MockHttpServletRequest req=new MockHttpServletRequest();
  UUID wedding=UUID.randomUUID();
  req.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of("weddingId",wedding.toString()));
  assertEquals(wedding,ApiAuditInterceptor.subjectId(req));
  req.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of("weddingId","invalid"));
  assertNull(ApiAuditInterceptor.subjectId(req));
 }
}
