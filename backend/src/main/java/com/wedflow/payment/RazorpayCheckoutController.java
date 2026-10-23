package com.wedflow.payment;
import com.wedflow.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/razorpay")
public class RazorpayCheckoutController {
 private final JdbcTemplate db;private final RazorpayClient razorpay;
 public RazorpayCheckoutController(JdbcTemplate db,RazorpayClient razorpay){this.db=db;this.razorpay=razorpay;}
 public record OrderInput(@NotNull UUID paymentIntentId){}
 @PostMapping("/orders") @Transactional public Map<String,Object> order(@Valid @RequestBody OrderInput in){
  var rows=db.query("SELECT p.amount_paise,p.status,p.razorpay_order_id FROM payment_records p JOIN vendor_bookings b ON b.id=p.booking_id JOIN weddings w ON w.id=b.wedding_id WHERE p.id=? AND w.owner_id=? FOR UPDATE OF p",(rs,n)->new Object[]{rs.getLong(1),rs.getString(2),rs.getString(3)},in.paymentIntentId(),SecurityConfig.currentUser());
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  Object[] row=rows.get(0);if(!"PENDING".equals(row[1]))throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment is not pending");
  String orderId=(String)row[2];
  if(orderId==null){var remote=razorpay.post("/orders",Map.of("amount",row[0],"currency","INR","receipt",in.paymentIntentId().toString().substring(0,32)));orderId=remote.path("id").asText();if(!orderId.startsWith("order_"))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);db.update("UPDATE payment_records SET razorpay_order_id=? WHERE id=?",orderId,in.paymentIntentId());}
  return Map.of("orderId",orderId,"keyId",razorpay.keyId(),"amountPaise",row[0],"currency","INR");
 }
}
