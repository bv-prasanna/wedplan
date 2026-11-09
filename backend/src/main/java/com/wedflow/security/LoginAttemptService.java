package com.wedflow.security;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Locale;

@Service
public class LoginAttemptService {
 private final JdbcTemplate db;
 public LoginAttemptService(JdbcTemplate db){this.db=db;}
 private String key(String email){return SecurityConfig.sha256(email.trim().toLowerCase(Locale.ROOT));}
 public void check(String email){
  Boolean blocked=db.query("SELECT blocked_until > CURRENT_TIMESTAMP FROM auth_login_attempts WHERE email_hash=?",
   (rs,n)->rs.getBoolean(1),key(email)).stream().findFirst().orElse(false);
  if(Boolean.TRUE.equals(blocked))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many login attempts. Please try again later.");
 }
 public void failed(String email){
  db.update("""
   INSERT INTO auth_login_attempts(email_hash,failed_attempts,window_started_at,blocked_until)
   VALUES (?,1,CURRENT_TIMESTAMP,NULL)
   ON CONFLICT(email_hash) DO UPDATE SET
    failed_attempts=CASE WHEN auth_login_attempts.window_started_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes' THEN 1 ELSE auth_login_attempts.failed_attempts+1 END,
    window_started_at=CASE WHEN auth_login_attempts.window_started_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes' THEN CURRENT_TIMESTAMP ELSE auth_login_attempts.window_started_at END,
    blocked_until=CASE WHEN auth_login_attempts.window_started_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes' THEN NULL
      WHEN auth_login_attempts.failed_attempts+1 >= 5 THEN CURRENT_TIMESTAMP + INTERVAL '15 minutes'
      ELSE auth_login_attempts.blocked_until END
   """,key(email));
 }
 public void succeeded(String email){db.update("DELETE FROM auth_login_attempts WHERE email_hash=?",key(email));}
}
