package com.wedflow.payment;

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

/**
 * Refunds are sensitive finance actions: customers cannot decide their own
 * refund amounts. Submission reserves a refund locally; a separate worker
 * submits the provider request after the database transaction has committed.
 */
@RestController
@RequestMapping("/api/v1/weddings/{weddingId}/refunds")
public class RefundController {
  private final JdbcTemplate db;
  public RefundController(JdbcTemplate db){this.db=db;}
  public record RefundRequest(@NotNull UUID paymentId,
      @Positive long amountPaise,
      @PositiveOrZero long deductionPaise,
      @NotBlank @Size(max=500) String reason,
      @NotBlank @Size(max=80) String requestKey) {}

  private void requireAdmin(){
    String role=db.queryForObject("SELECT role FROM app_users WHERE id=?",String.class,SecurityConfig.currentUser());
    if(!"ADMIN".equals(role)&&!"SUPER_ADMIN".equals(role))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Refunds require administrator approval");
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Transactional
  public Map<String,Object> refund(@PathVariable UUID weddingId,@Valid @RequestBody RefundRequest input){
    requireAdmin();
    if(!input.requestKey().matches("[A-Za-z0-9_-]{8,80}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid request reference");
    var rows=db.query("""
      SELECT p.razorpay_payment_id,p.amount_paise,b.status
      FROM payment_records p
      JOIN vendor_bookings b ON b.id=p.booking_id
      JOIN weddings w ON w.id=b.wedding_id
      WHERE w.id=? AND p.id=? AND p.status='VERIFIED'
      FOR UPDATE OF p
      """,(rs,n)->new Object[]{rs.getString(1),rs.getLong(2),rs.getString(3)},weddingId,input.paymentId());
    if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Verified payment not found");
    Object[] row=rows.get(0);
    if(row[0]==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment not captured");
    if(!"CANCELLED".equals(row[2]))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Cancel the booking and confirm refund policy before requesting a refund");

    List<Map<String,Object>> prior=db.query("""
      SELECT id,status,amount_paise FROM payment_refunds
      WHERE payment_id=? AND request_key=?
      """,(rs,n)->Map.of("id",rs.getObject(1,UUID.class),"status",rs.getString(2),
          "amountPaise",rs.getLong(3)),input.paymentId(),input.requestKey());
    if(!prior.isEmpty()){
      var existing=prior.get(0);
      if((long)existing.get("amountPaise")!=input.amountPaise())
        throw new ResponseStatusException(HttpStatus.CONFLICT,"Request reference already used for a different amount");
      return existing;
    }

    Long reserved=db.queryForObject("""
      SELECT COALESCE(SUM(amount_paise),0) FROM payment_refunds
      WHERE payment_id=? AND status IN
      ('QUEUED','PROCESSING','PENDING','PROCESSED','REQUIRES_RECONCILIATION')
      """,Long.class,input.paymentId());
    if(reserved==null||reserved<0||(long)row[1]<reserved)
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Refund reconciliation required");
    long available=(long)row[1]-reserved;
    if(input.amountPaise()>available ||
       input.deductionPaise()>available-input.amountPaise())
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Refund and deduction exceed available balance");

    UUID id=UUID.randomUUID();
    db.update("""
      INSERT INTO payment_refunds(id,payment_id,amount_paise,status,requested_by,
         reason,deduction_paise,request_key)
      VALUES(?,?,?,'QUEUED',?,?,?,?)
      """,id,input.paymentId(),input.amountPaise(),SecurityConfig.currentUser(),
          input.reason().trim(),input.deductionPaise(),input.requestKey());
    return Map.of("id",id,"amountPaise",input.amountPaise(),
      "status","QUEUED","message","Refund request queued for provider submission");
  }
}
