package com.wedflow.notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component public class NotificationWorker {
 private final JdbcTemplate db;private final EmailSender email;
 public NotificationWorker(JdbcTemplate db,EmailSender email){this.db=db;this.email=email;}
 @Scheduled(fixedDelayString="${NOTIFY_POLL_MS:60000}")
 public void deliver(){
  var rows=db.query("SELECT id,recipient,channel,subject,body FROM notification_outbox WHERE status='PENDING' AND attempts<5 ORDER BY created_at LIMIT 10",(rs,n)->new String[]{rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)});
  for(String[] row:rows){
   UUID id=UUID.fromString(row[0]);
   if(db.update("UPDATE notification_outbox SET status='SENDING',attempts=attempts+1 WHERE id=? AND status='PENDING'",id)==0)continue;
   try{
    if(!"EMAIL".equals(row[2]))throw new IllegalStateException("SMS delivery provider not configured");
    email.send(row[1],row[3],row[4]);
    db.update("UPDATE notification_outbox SET status='SENT',sent_at=now() WHERE id=?",id);
   }catch(Exception e){db.update("UPDATE notification_outbox SET status=CASE WHEN attempts>=5 THEN 'FAILED' ELSE 'PENDING' END WHERE id=?",id);}
  }
 }
}
