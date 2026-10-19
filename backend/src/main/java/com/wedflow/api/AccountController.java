package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/account")
public class AccountController {
 private final JdbcTemplate db;public AccountController(JdbcTemplate db){this.db=db;}
 public record Profile(UUID id,String email,String role){}
 @GetMapping("/me") public Profile me(){return db.queryForObject("SELECT id,email,role FROM app_users WHERE id=?",(rs,n)->new Profile(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3)),SecurityConfig.currentUser());}
 @PostMapping("/logout-all") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void logoutAll(){db.update("DELETE FROM auth_tokens WHERE user_id=?",SecurityConfig.currentUser());}
}
