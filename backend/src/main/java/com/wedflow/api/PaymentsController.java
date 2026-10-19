package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/weddings/{weddingId}/payments")
public class PaymentsController {
 private final JdbcTemplate db;public PaymentsController(JdbcTemplate db){this.db=db;}
 private void owner(UUID wedding){Integer n=db.queryForObject("SELECT COUNT(*) FROM weddings WHERE id=? AND owner_id=?",Integer.class,wedding,SecurityConfig.currentUser());if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 public record PaymentIntent(@NotNull UUID bookingId,@Positive long amountPaise){}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,Object> create(@PathVariable UUID weddingId,@Valid @RequestBody PaymentIntent in){
  owner(weddingId);Integer n=db.queryForObject("SELECT COUNT(*) FROM vendor_bookings WHERE id=? AND wedding_id=? AND status='CONFIRMED'",Integer.class,in.bookingId(),weddingId);if(n==null||n==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking not payable");
  // Lock the booking row so concurrent payment-intent requests cannot exceed the quote.
  db.query("SELECT id FROM vendor_bookings WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),in.bookingId());
  Long due=db.queryForObject("SELECT q.amount_paise FROM vendor_bookings b JOIN vendor_quotes q ON b.quote_id=q.id WHERE b.id=?",Long.class,in.bookingId());
  Long reserved=db.queryForObject("SELECT COALESCE(SUM(amount_paise),0) FROM payment_records WHERE booking_id=? AND status IN ('PENDING','VERIFIED')",Long.class,in.bookingId());
  if(due==null||reserved==null||in.amountPaise()>due-reserved)throw new ResponseStatusException(HttpStatus.CONFLICT,"Amount exceeds unpaid balance");
  UUID id=UUID.randomUUID();db.update("INSERT INTO payment_records(id,booking_id,amount_paise) VALUES(?,?,?)",id,in.bookingId(),in.amountPaise());return Map.of("id",id,"status","PENDING","amountPaise",in.amountPaise());
 }
 @GetMapping public List<Map<String,Object>> list(@PathVariable UUID weddingId){owner(weddingId);return db.query("SELECT p.id,p.booking_id,p.amount_paise,p.status FROM payment_records p JOIN vendor_bookings b ON p.booking_id=b.id WHERE b.wedding_id=? ORDER BY p.created_at DESC",(rs,n)->Map.of("id",rs.getObject(1,UUID.class),"bookingId",rs.getObject(2,UUID.class),"amountPaise",rs.getLong(3),"status",rs.getString(4)),weddingId);}
}
