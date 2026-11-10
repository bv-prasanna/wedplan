package com.wedflow.payment;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefundDispatchWorkerTest {
 @Test void unclaimedRefundIsNeverSentToProvider(){
  var db=mock(JdbcTemplate.class);
  var provider=mock(RazorpayClient.class);
  UUID id=UUID.randomUUID();
  when(db.update(contains("status='PROCESSING'"),eq(id))).thenReturn(0);
  new RefundDispatchWorker(db,provider).submit(
    new RefundDispatchWorker.Queued(id,900,"pay_1234567"));
  verifyNoInteractions(provider);
 }
 @Test void timeoutRequiresManualReconciliationInsteadOfAutomaticRetry(){
  var db=mock(JdbcTemplate.class);
  var provider=mock(RazorpayClient.class);
  UUID id=UUID.randomUUID();
  when(db.update(contains("status='PROCESSING'"),eq(id))).thenReturn(1);
  when(provider.post(anyString(),anyMap())).thenThrow(new IllegalStateException("network timeout"));
  new RefundDispatchWorker(db,provider).submit(
    new RefundDispatchWorker.Queued(id,900,"pay_1234567"));
  verify(provider,times(1)).post(eq("/payments/pay_1234567/refund"),anyMap());
  verify(db).update(contains("REQUIRES_RECONCILIATION"),eq(id));
 }
}
