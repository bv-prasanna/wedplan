package com.wedflow.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Durable at-least-once email delivery. Atomic row claims prevent concurrent
 * worker duplication. Stale SENDING leases are recovered after five minutes.
 * A crash after provider acceptance may still cause one duplicate email;
 * delivery is never proof that a booking/payment/refund succeeded.
 */
@Component
public class NotificationWorker {
  private static final Logger LOG=LoggerFactory.getLogger(NotificationWorker.class);
  private final JdbcTemplate db;
  private final EmailSender email;
  public NotificationWorker(JdbcTemplate db,EmailSender email){this.db=db;this.email=email;}

  @Scheduled(fixedDelayString="${NOTIFY_POLL_MS:60000}")
  public void deliver(){
    db.update("""
      UPDATE notification_outbox SET
        status=CASE WHEN attempts>=5 THEN 'FAILED' ELSE 'PENDING' END,
        next_attempt_at=now()+INTERVAL '1 minute',
        last_error_type='STALE_SENDING_RECOVERED'
      WHERE status='SENDING' AND
        (claimed_at IS NULL OR claimed_at<now()-INTERVAL '5 minutes')
      """);
    var rows=db.query("""
      SELECT id,recipient,channel,subject,body FROM notification_outbox
      WHERE status='PENDING' AND attempts<5 AND next_attempt_at<=now()
      ORDER BY created_at LIMIT 10
      """,(rs,n)->new String[]{rs.getString(1),rs.getString(2),
        rs.getString(3),rs.getString(4),rs.getString(5)});

    for(String[] row:rows){
      UUID id=UUID.fromString(row[0]);
      if(db.update("""
        UPDATE notification_outbox
        SET status='SENDING',claimed_at=now(),attempts=attempts+1
        WHERE id=? AND status='PENDING' AND attempts<5 AND next_attempt_at<=now()
        """,id)!=1)continue;
      if(!"EMAIL".equals(row[2])){
        db.update("""
          UPDATE notification_outbox SET status='FAILED',
            last_error_type='CHANNEL_NOT_CONFIGURED'
          WHERE id=? AND status='SENDING'
          """,id);
        continue;
      }
      try{
        email.send(row[1],row[3],row[4]);
        db.update("""
          UPDATE notification_outbox SET status='SENT',sent_at=now(),
            last_error_type=NULL WHERE id=? AND status='SENDING'
          """,id);
      }catch(Exception e){
        String type=e.getClass().getSimpleName().replaceAll("[^A-Za-z0-9_]","_");
        db.update("""
          UPDATE notification_outbox SET
            status=CASE WHEN attempts>=5 THEN 'FAILED' ELSE 'PENDING' END,
            next_attempt_at=now()+(LEAST(attempts*attempts,60)*INTERVAL '1 minute'),
            last_error_type=?
          WHERE id=? AND status='SENDING'
          """,type.length()>80?type.substring(0,80):type,id);
        LOG.warn("Notification delivery deferred id={} category={}",id,type);
      }
    }
  }
}
