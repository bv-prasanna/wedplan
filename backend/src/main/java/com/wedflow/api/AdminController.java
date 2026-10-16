package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/admin")
public class AdminController {
 private final JdbcTemplate db;public AdminController(JdbcTemplate db){this.db=db;}
 private void requireAdmin(){String role=db.queryForObject("SELECT role FROM app_users WHERE id=?",String.class,SecurityConfig.currentUser());if(!"ADMIN".equals(role)&&!"SUPER_ADMIN".equals(role))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 @PostMapping("/vendors/{vendorId}/approve") @Transactional public Map<String,Object> approve(@PathVariable UUID vendorId){requireAdmin();if(db.update("UPDATE vendor_profiles SET verification_status='APPROVED' WHERE id=? AND verification_status='PENDING'",vendorId)==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Vendor not pending");db.update("UPDATE vendors SET approved=true WHERE id=?",vendorId);db.update("INSERT INTO audit_events(id,actor_id,action,subject_id) VALUES(?,?,?,?)",UUID.randomUUID(),SecurityConfig.currentUser(),"VENDOR_APPROVED",vendorId);return Map.of("vendorId",vendorId,"status","APPROVED");}
 @PostMapping("/vendors/{vendorId}/reject") @Transactional public Map<String,Object> reject(@PathVariable UUID vendorId){requireAdmin();if(db.update("UPDATE vendor_profiles SET verification_status='REJECTED' WHERE id=? AND verification_status='PENDING'",vendorId)==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Vendor not pending");db.update("UPDATE vendors SET approved=false WHERE id=?",vendorId);db.update("INSERT INTO audit_events(id,actor_id,action,subject_id) VALUES(?,?,?,?)",UUID.randomUUID(),SecurityConfig.currentUser(),"VENDOR_REJECTED",vendorId);return Map.of("vendorId",vendorId,"status","REJECTED");}
}
