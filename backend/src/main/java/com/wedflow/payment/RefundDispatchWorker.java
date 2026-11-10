package com.wedflow.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * At-most-once automatic submission. Provider calls only happen after the
 * QUEUED request has been committed and atomically claimed as PROCESSING.
 *
 * Unknown provider outcomes are REQUIRES_RECONCILIATION, never auto-retried.
 * This prevents duplicate refunds after a timeout/crash, at the expense of
 * requiring manual provider reconciliation for ambiguous outcomes.
 */
@Component
public class RefundDispatchWorker {
  private static final Logger LOG=LoggerFactory.getLogger(RefundDispatchWorker.class);
  private final JdbcTemplate db;
  private final RazorpayClient provider;
  public RefundDispatchWorker(JdbcTemplate db,RazorpayClient provider){this.db=db;this.provider=provider;}
  public record Queued(UUID id,long amountPaise,String providerPaymentId){}

  @Scheduled(fixedDelayString="${REFUND_POLL_MS:60000}")
  public void dispatch(){
    List<Queued> rows=db.query("""
      SELECT r.id,r.amount_paise,p.razorpay_payment_id
      FROM payment_refunds r JOIN payment_records p ON p.id=r.payment_id
      WHERE r.status='QUEUED' AND p.status='VERIFIED'
      ORDER BY r.created_at LIMIT 10
      """,(rs,n)->new Queued(rs.getObject(1,UUID.class),rs.getLong(2),rs.getString(3)));
    for(Queued row:rows)submit(row);
  }

  void submit(Queued row){
    if(db.update("""
      UPDATE payment_refunds SET status='PROCESSING',dispatched_at=now()
      WHERE id=? AND status='QUEUED'
      """,row.id())!=1)return;
    try{
      if(row.providerPaymentId()==null || !row.providerPaymentId().matches("pay_[A-Za-z0-9]+"))
        throw new IllegalStateException("Provider payment ID unavailable");
      var remote=provider.post("/payments/"+row.providerPaymentId()+"/refund",
        Map.of("amount",row.amountPaise(),
          "notes",Map.of("wedflow_refund_id",row.id().toString())));
      String providerRefundId=remote.path("id").asText();
      if(!providerRefundId.matches("rfnd_[A-Za-z0-9]+"))
        throw new IllegalStateException("Unrecognized provider refund response");
      // Do not override a refund webhook that completed while the API call was in flight.
      int changed=db.update("""
        UPDATE payment_refunds SET razorpay_refund_id=?,status='PENDING'
        WHERE id=? AND status='PROCESSING'
        """,providerRefundId,row.id());
      if(changed==0)LOG.info("Refund status reconciled by another handler refundId={}",row.id());
    }catch(Exception ex){
      // Provider may already have accepted the payment before the connection failed:
      // never resubmit this row automatically.
      db.update("""
        UPDATE payment_refunds SET status='REQUIRES_RECONCILIATION'
        WHERE id=? AND status='PROCESSING'
        """,row.id());
      LOG.warn("Refund requires provider reconciliation refundId={} failureType={}",
        row.id(),ex.getClass().getSimpleName());
    }
  }
}
