package com.wedflow.payment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefundControllerSecurityTest {
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
 @Test void customerCannotIssueSelfApprovedRefund(){
  var db=mock(JdbcTemplate.class);
  UUID user=UUID.randomUUID();
  SecurityContextHolder.getContext().setAuthentication(
    new UsernamePasswordAuthenticationToken(user,null,List.of()));
  when(db.queryForObject(startsWith("SELECT role FROM app_users"),eq(String.class),eq(user)))
      .thenReturn("CUSTOMER");
  var controller=new RefundController(db);
  var request=new RefundController.RefundRequest(UUID.randomUUID(),7500L,2500L,
    "Cancellation with documented service fee","test_request_key_12345");
  var error=assertThrows(ResponseStatusException.class,
    ()->controller.refund(UUID.randomUUID(),request));
  assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode());
  verify(db,never()).update(anyString(),any(Object[].class));
 }
 @Test void administratorMustProvideValidIdempotencyReference(){
  var db=mock(JdbcTemplate.class);
  UUID user=UUID.randomUUID();
  SecurityContextHolder.getContext().setAuthentication(
    new UsernamePasswordAuthenticationToken(user,null,List.of()));
  when(db.queryForObject(startsWith("SELECT role FROM app_users"),eq(String.class),eq(user)))
      .thenReturn("ADMIN");
  var controller=new RefundController(db);
  var request=new RefundController.RefundRequest(UUID.randomUUID(),1000L,0,
    "Cancellation","invalid !");
  var error=assertThrows(ResponseStatusException.class,
    ()->controller.refund(UUID.randomUUID(),request));
  assertEquals(HttpStatus.BAD_REQUEST,error.getStatusCode());
  verify(db,never()).update(anyString(),any(Object[].class));
 }
}
