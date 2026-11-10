package com.wedflow.payment;

import com.wedflow.security.SecurityConfig;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Finance-only support queues: never expose arbitrary customer PII. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminRefundQueueController {
  private final JdbcTemplate db;
  public AdminRefundQueueController(JdbcTemplate db){this.db=db;}
  private void admin(){
    String role=db.queryForObject("SELECT role FROM app_users WHERE id=?",String.class,SecurityConfig.currentUser());
    if(!"ADMIN".equals(role)&&!"SUPER_ADMIN".equals(role))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  public record RefundablePayment(UUID paymentId,UUID weddingId,String weddingTitle,
      long amountPaise,long reservedPaise,LocalDate serviceDate) {}
  public record RefundRecord(UUID id,UUID paymentId,String weddingTitle,
      long amountPaise,long deductionPaise,String status,String reason,OffsetDateTime requestedAt) {}

  @GetMapping("/refundable-payments")
  public List<RefundablePayment> refundablePayments(){
    admin();
    return db.query("""
      SELECT p.id,w.id,w.title,p.amount_paise,
        COALESCE((SELECT SUM(r.amount_paise) FROM payment_refunds r
          WHERE r.payment_id=p.id AND r.status IN
          ('QUEUED','PROCESSING','PENDING','PROCESSED','REQUIRES_RECONCILIATION')),0),
        b.service_date
      FROM payment_records p
      JOIN vendor_bookings b ON b.id=p.booking_id
      JOIN weddings w ON w.id=b.wedding_id
      WHERE p.status='VERIFIED' AND b.status='CANCELLED'
      ORDER BY p.created_at DESC LIMIT 150
      """,(rs,n)->new RefundablePayment(
        rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),
        rs.getLong(4),rs.getLong(5),rs.getObject(6,LocalDate.class)
      )).stream().filter(p->p.amountPaise()>p.reservedPaise()).toList();
  }

  @GetMapping("/refund-requests")
  public List<RefundRecord> refundRequests(){
    admin();
    return db.query("""
      SELECT r.id,r.payment_id,w.title,r.amount_paise,r.deduction_paise,
        r.status,r.reason,r.created_at
      FROM payment_refunds r
      JOIN payment_records p ON p.id=r.payment_id
      JOIN vendor_bookings b ON b.id=p.booking_id
      JOIN weddings w ON w.id=b.wedding_id
      ORDER BY r.created_at DESC LIMIT 150
      """,(rs,n)->new RefundRecord(
        rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),
        rs.getLong(4),rs.getLong(5),rs.getString(6),rs.getString(7),
        rs.getObject(8,OffsetDateTime.class)
      ));
  }
}
