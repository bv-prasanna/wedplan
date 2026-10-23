package com.wedflow.payment;
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
@RestController @RequestMapping("/api/v1/weddings/{weddingId}/refunds")
public class RefundController {
 private final JdbcTemplate db;private final RazorpayClient client;
 public RefundController(JdbcTemplate db,RazorpayClient client){this.db=db;this.client=client;}
 public record RefundRequest(@NotNull UUID paymentId,@Positive long amountPaise){}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,Object> refund(@PathVariable UUID weddingId,@Valid @RequestBody RefundRequest input){
  var rows=db.query("SELECT p.razorpay_payment_id,p.amount_paise FROM payment_records p JOIN vendor_bookings b ON b.id=p.booking_id JOIN weddings w ON w.id=b.wedding_id WHERE w.id=? AND w.owner_id=? AND p.id=? AND p.status='VERIFIED' FOR UPDATE OF p",(rs,n)->new Object[]{rs.getString(1),rs.getLong(2)},weddingId,SecurityConfig.currentUser(),input.paymentId());
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var row=rows.get(0);if(row[0]==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment not captured");
  Long refunded=db.queryForObject("SELECT COALESCE(SUM(amount_paise),0) FROM payment_refunds WHERE payment_id=? AND status IN ('PENDING','PROCESSED')",Long.class,input.paymentId());
  if(refunded==null||input.amountPaise()>(long)row[1]-refunded)throw new ResponseStatusException(HttpStatus.CONFLICT,"Refund exceeds balance");
  UUID id=UUID.randomUUID();db.update("INSERT INTO payment_refunds(id,payment_id,amount_paise) VALUES(?,?,?)",id,input.paymentId(),input.amountPaise());
  // Razorpay processes asynchronously. Never mark a refund processed from a request alone.
  var remote=client.post("/payments/"+row[0]+"/refund",Map.of("amount",input.amountPaise(),"notes",Map.of("wedflow_refund_id",id.toString())));
  String refundId=remote.path("id").asText();if(!refundId.startsWith("rfnd_"))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
  db.update("UPDATE payment_refunds SET razorpay_refund_id=? WHERE id=?",refundId,id);
  return Map.of("id",id,"razorpayRefundId",refundId,"status","PENDING");
 }
}
