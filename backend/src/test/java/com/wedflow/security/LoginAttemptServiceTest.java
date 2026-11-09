package com.wedflow.security;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.RowMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {
 @Test void deniesLockedAccountWithoutExposingEmail(){
  JdbcTemplate db=mock(JdbcTemplate.class);
  when(db.query(anyString(),any(RowMapper.class),anyString())).thenReturn(List.of(true));
  var service=new LoginAttemptService(db);
  var error=assertThrows(ResponseStatusException.class,()->service.check("CUSTOMER@example.com"));
  assertEquals(429,error.getStatusCode().value());
  assertFalse(error.getReason().contains("CUSTOMER@example.com"));
 }
 @Test void hashesIdentifierBeforePersistingFailure(){
  JdbcTemplate db=mock(JdbcTemplate.class);
  var service=new LoginAttemptService(db);
  service.failed("CUSTOMER@example.com");
  verify(db).update(anyString(),eq(SecurityConfig.sha256("customer@example.com")));
 }
 @Test void clearsFailuresAfterSuccessfulAuthentication(){
  JdbcTemplate db=mock(JdbcTemplate.class);
  new LoginAttemptService(db).succeeded("CUSTOMER@example.com");
  verify(db).update(startsWith("DELETE FROM auth_login_attempts"),eq(SecurityConfig.sha256("customer@example.com")));
 }
}
