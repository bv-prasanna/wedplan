package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1")
public class OperationsController {
 private final JdbcTemplate db;public OperationsController(JdbcTemplate db){this.db=db;}
 private void owner(UUID wedding){Integer n=db.queryForObject("SELECT COUNT(*) FROM weddings WHERE id=? AND owner_id=?",Integer.class,wedding,SecurityConfig.currentUser());if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 private void vendorOwner(UUID vendor){Integer n=db.queryForObject("SELECT COUNT(*) FROM vendor_profiles WHERE id=? AND owner_id=?",Integer.class,vendor,SecurityConfig.currentUser());if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 public record DateInput(@NotNull LocalDate date){}
 public record ReviewInput(@Min(1) @Max(5) int rating,@Size(max=1000) String comment){}
 @PostMapping("/vendor-profiles/{vendorId}/unavailable") @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,Object> unavailable(@PathVariable UUID vendorId,@Valid @RequestBody DateInput in){vendorOwner(vendorId);UUID id=UUID.randomUUID();db.update("INSERT INTO vendor_unavailability(id,vendor_id,unavailable_date) VALUES(?,?,?) ON CONFLICT(vendor_id,unavailable_date) DO NOTHING",id,vendorId,in.date());return Map.of("vendorId",vendorId,"date",in.date());}
 @GetMapping("/vendors/{vendorId}/availability") public Map<String,Object> availability(@PathVariable UUID vendorId,@RequestParam LocalDate date){
  Integer approved=db.queryForObject("SELECT COUNT(*) FROM vendors WHERE id=? AND approved=true",Integer.class,vendorId);
  Integer busy=db.queryForObject("SELECT COUNT(*) FROM vendor_bookings WHERE vendor_id=? AND service_date=? AND status='CONFIRMED'",Integer.class,vendorId,date);
  Integer blocked=db.queryForObject("SELECT COUNT(*) FROM vendor_unavailability WHERE vendor_id=? AND unavailable_date=?",Integer.class,vendorId,date);
  return Map.of("vendorId",vendorId,"date",date,"available",approved!=null&&approved>0&&busy!=null&&busy==0&&blocked!=null&&blocked==0);
 }
 @PostMapping("/weddings/{weddingId}/bookings/{bookingId}/reviews") @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,Object> review(@PathVariable UUID weddingId,@PathVariable UUID bookingId,@Valid @RequestBody ReviewInput in){
  owner(weddingId);Integer n=db.queryForObject("SELECT COUNT(*) FROM vendor_bookings WHERE id=? AND wedding_id=? AND status='COMPLETED'",Integer.class,bookingId,weddingId);if(n==null||n==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Only completed bookings can be reviewed");
  UUID id=UUID.randomUUID();db.update("INSERT INTO vendor_reviews(id,booking_id,rating,comment) VALUES(?,?,?,?)",id,bookingId,in.rating(),in.comment());return Map.of("id",id,"rating",in.rating());
 }
 @GetMapping("/notifications") public List<Map<String,Object>> notifications(){return db.query("SELECT id,kind,title,body,read_at IS NOT NULL FROM user_notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 100",(rs,n)->Map.of("id",rs.getObject(1,UUID.class),"kind",rs.getString(2),"title",rs.getString(3),"body",rs.getString(4),"read",rs.getBoolean(5)),SecurityConfig.currentUser());}
 @PatchMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void markRead(@PathVariable UUID id){if(db.update("UPDATE user_notifications SET read_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?",id,SecurityConfig.currentUser())==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
}
