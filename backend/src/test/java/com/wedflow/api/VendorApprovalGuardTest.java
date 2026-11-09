package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
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

class VendorApprovalGuardTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void pendingVendorCannotIssueQuote(){
  UUID vendor=UUID.randomUUID(),user=UUID.randomUUID();
  SecurityContextHolder.getContext().setAuthentication(
    new UsernamePasswordAuthenticationToken(user,null,List.of()));
  JdbcTemplate db=mock(JdbcTemplate.class);
  when(db.queryForObject(contains("FROM vendor_profiles WHERE id=? AND owner_id=?"),
    eq(Integer.class),eq(vendor),eq(user))).thenReturn(1);
  when(db.queryForObject(contains("v.approved=true"),eq(Integer.class),eq(vendor)))
    .thenReturn(0);
  CommerceController controller=new CommerceController(db);
  ResponseStatusException error=assertThrows(ResponseStatusException.class,
    ()->controller.quote(vendor,new CommerceController.QuoteInput(
      UUID.randomUUID(),10000,"Service details")));
  assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
  verify(db,never()).update(anyString(),any(Object[].class));
 }
}
