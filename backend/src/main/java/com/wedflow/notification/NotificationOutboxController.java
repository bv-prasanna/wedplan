package com.wedflow.notification;
import com.wedflow.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/notifications")
public class NotificationOutboxController {
 private final JdbcTemplate db;public NotificationOutboxController(JdbcTemplate db){this.db=db;}
 public record Message(@NotBlank @Email String email,@NotBlank @Size(max=200) String subject,@NotBlank @Size(max=4000) String body){}
 @PostMapping("/email") @ResponseStatus(HttpStatus.ACCEPTED) @Transactional public Map<String,Object> email(@Valid @RequestBody Message input){
  String own=db.queryForObject("SELECT email FROM app_users WHERE id=?",String.class,SecurityConfig.currentUser());
  if(!own.equalsIgnoreCase(input.email()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Can only notify own verified address");
  UUID id=UUID.randomUUID();db.update("INSERT INTO notification_outbox(id,recipient,channel,subject,body) VALUES(?,?,'EMAIL',?,?)",id,own,input.subject(),input.body());return Map.of("id",id,"status","QUEUED");
 }
}
