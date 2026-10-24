package com.wedflow.audit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service public class AuditEventService {
 private final JdbcTemplate db;public AuditEventService(JdbcTemplate db){this.db=db;}
 public void record(UUID actor,String action,UUID subject,EventLocation location){
  EventLocation geo=location==null?EventLocation.unavailable("NOT_PROVIDED"):location;
  db.update("INSERT INTO audit_events(id,actor_id,action,subject_id,latitude,longitude,location_source,location_accuracy_m,location_recorded_at,location_unavailable_reason) VALUES(?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),actor,action,subject,geo.latitude(),geo.longitude(),geo.source(),geo.accuracyMeters(),geo.recordedAt(),geo.unavailableReason());
 }
}
