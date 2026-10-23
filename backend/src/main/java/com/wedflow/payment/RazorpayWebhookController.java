package com.wedflow.payment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/razorpay")
public class RazorpayWebhookController {
 private final String secret;private final JdbcTemplate db;private final ObjectMapper mapper;
 public RazorpayWebhookController(@Value("${RAZORPAY_WEBHOOK_SECRET:}") String secret,JdbcTemplate db,ObjectMapper mapper){this.secret=secret;this.db=db;this.mapper=mapper;}
 @PostMapping("/webhook") @Transactional public void webhook(@RequestHeader("X-Razorpay-Signature") String signature,@RequestHeader("X-Razorpay-Event-Id") String eventId,@RequestBody String body)throws Exception{
  if(!RazorpaySignature.verify(secret,body,signature))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid signature");
  if(eventId.isBlank()||eventId.length()>200)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  JsonNode payload=mapper.readTree(body);String event=payload.path("event").asText();
  JsonNode payment=payload.path("payload").path("payment").path("entity");
  String providerPaymentId=payment.path("id").asText();
  String orderId=payment.path("order_id").asText();
  if(event.equals("refund.processed")||event.equals("refund.failed")){
    JsonNode refund=payload.path("payload").path("refund").path("entity");
    String refundId=refund.path("id").asText();
    if(!refundId.startsWith("rfnd_"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid refund id");
    var refunds=db.query("SELECT r.id,r.amount_paise,p.razorpay_payment_id FROM payment_refunds r JOIN payment_records p ON p.id=r.payment_id WHERE r.razorpay_refund_id=? FOR UPDATE OF r",(rs,n)->new Object[]{rs.getObject(1,UUID.class),rs.getLong(2),rs.getString(3)},refundId);
    if(refunds.isEmpty())return;
    Object[] row=refunds.get(0);
    if(refund.path("amount").asLong(-1)!=(long)row[1]||!refund.path("payment_id").asText().equals(row[2])||!"INR".equals(refund.path("currency").asText()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Refund mismatch");
    int inserted=db.update("INSERT INTO payment_events(id,payment_id,provider_event_id,event_type) SELECT ?,payment_id,?,? FROM payment_refunds WHERE id=? ON CONFLICT(provider_event_id) DO NOTHING",UUID.randomUUID(),eventId,event,(UUID)row[0]);
    if(inserted==0)return;
    db.update("UPDATE payment_refunds SET status=? WHERE id=? AND status='PENDING'",event.equals("refund.processed")?"PROCESSED":"FAILED",(UUID)row[0]);
    return;
  }
  if(!event.equals("payment.captured")&&!event.equals("payment.failed"))return;
  if(providerPaymentId.isBlank()||orderId.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var records=db.query("SELECT id,amount_paise FROM payment_records WHERE razorpay_order_id=? FOR UPDATE",(rs,n)->new Object[]{rs.getObject(1,UUID.class),rs.getLong(2)},orderId);
  if(records.isEmpty())return;
  UUID id=(UUID)records.get(0)[0];long expected=(long)records.get(0)[1];
  if(payment.path("amount").asLong(-1)!=expected||!"INR".equals(payment.path("currency").asText()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment amount mismatch");
  int inserted=db.update("INSERT INTO payment_events(id,payment_id,provider_event_id,event_type) VALUES(?,?,?,?) ON CONFLICT(provider_event_id) DO NOTHING",UUID.randomUUID(),id,eventId,event);
  if(inserted==0)return;
  if(event.equals("payment.captured"))db.update("UPDATE payment_records SET status='VERIFIED',razorpay_payment_id=? WHERE id=? AND status='PENDING'",providerPaymentId,id);
  else db.update("UPDATE payment_records SET status='FAILED' WHERE id=? AND status='PENDING'",id);
 }
}
