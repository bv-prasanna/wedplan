package com.wedflow.notification;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationWorkerRecoveryTest {
 @Test void scheduledCycleRecoversStaleLeasesBeforeReadingQueuedRows(){
  var db=mock(JdbcTemplate.class);
  var email=mock(EmailSender.class);
  when(db.query(anyString(),any(org.springframework.jdbc.core.RowMapper.class)))
    .thenReturn(List.of());
  new NotificationWorker(db,email).deliver();
  verify(db).update(contains("STALE_SENDING_RECOVERED"));
  verify(db).query(contains("next_attempt_at<=now()"),any(org.springframework.jdbc.core.RowMapper.class));
  verifyNoInteractions(email);
 }
}
