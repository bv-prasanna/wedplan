package com.wedflow.api;

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

class AdminWorkspaceSecurityTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void customerCannotViewAdminSummary(){
  JdbcTemplate db=mock(JdbcTemplate.class);
  SecurityContextHolder.getContext().setAuthentication(
    new UsernamePasswordAuthenticationToken(UUID.randomUUID(),null,List.of()));
  doReturn(List.of("CUSTOMER")).when(db).query(
    startsWith("SELECT role"),any(org.springframework.jdbc.core.RowMapper.class),any(UUID.class));
  ResponseStatusException error=assertThrows(ResponseStatusException.class,
    ()->new AdminDashboardController(db).summary());
  assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode());
 }
 @Test void missingVendorOwnerCannotViewEnquiries(){
  JdbcTemplate db=mock(JdbcTemplate.class);
  UUID vendor=UUID.randomUUID();
  SecurityContextHolder.getContext().setAuthentication(
    new UsernamePasswordAuthenticationToken(UUID.randomUUID(),null,List.of()));
  when(db.queryForObject(startsWith("SELECT COUNT(*) FROM vendor_profiles"),eq(Integer.class),
    eq(vendor),any(UUID.class))).thenReturn(0);
  ResponseStatusException error=assertThrows(ResponseStatusException.class,
    ()->new VendorWorkspaceController(db).enquiries(vendor));
  assertEquals(HttpStatus.NOT_FOUND,error.getStatusCode());
 }
}
