package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final JdbcTemplate db;private final PasswordEncoder encoder;private final SecureRandom random=new SecureRandom();
 public AuthController(JdbcTemplate db,PasswordEncoder encoder){this.db=db;this.encoder=encoder;}
 public record Credentials(@NotBlank @Email String email,@NotBlank @Size(min=12,max=128) String password){}
 public record TokenResponse(String accessToken,String tokenType,long expiresInSeconds){}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public Map<String,String> register(@Valid @RequestBody Credentials in){
   UUID id=UUID.randomUUID();String email=in.email().trim().toLowerCase(Locale.ROOT);
   try{db.update("INSERT INTO app_users(id,email,password_hash,role) VALUES(?,?,?,?)",id,email,encoder.encode(in.password()),"CUSTOMER");}
   catch(DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Account already exists");}
   return Map.of("id",id.toString(),"email",email);
 }
 @PostMapping("/login") @Transactional
 public TokenResponse login(@Valid @RequestBody Credentials in){
   var users=db.query("SELECT id,password_hash FROM app_users WHERE email=?", (rs,n)->Map.entry(rs.getObject("id",UUID.class),rs.getString("password_hash")),in.email().trim().toLowerCase(Locale.ROOT));
   if(users.isEmpty()||!encoder.matches(in.password(),users.get(0).getValue()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
   byte[] bytes=new byte[32];random.nextBytes(bytes);String token=HexFormat.of().formatHex(bytes);
   db.update("INSERT INTO auth_tokens(id,user_id,token_hash,expires_at) VALUES(?,?,?,?)",UUID.randomUUID(),users.get(0).getKey(),SecurityConfig.sha256(token),java.sql.Timestamp.from(Instant.now().plus(12,ChronoUnit.HOURS)));
   return new TokenResponse(token,"Bearer",43200);
 }
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 public void logout(@RequestHeader(value="Authorization",required=false) String authorization){
   if(authorization==null||!authorization.startsWith("Bearer "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
   db.update("DELETE FROM auth_tokens WHERE token_hash=? AND user_id=?",SecurityConfig.sha256(authorization.substring(7)),SecurityConfig.currentUser());
 }
}
