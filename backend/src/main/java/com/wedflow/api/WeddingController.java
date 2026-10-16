package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/weddings")
public class WeddingController {
 private final JdbcTemplate db;
 public WeddingController(JdbcTemplate db){this.db=db;}
 public record WeddingInput(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=120) String city,@NotNull LocalDate eventDate,@Positive long totalBudgetPaise){}
 public record CeremonyInput(@NotBlank @Size(max=120) String name,@NotNull LocalDate eventDate,@Size(max=250) String venue){}
 public record WeddingView(UUID id,String title,String city,LocalDate eventDate,long totalBudgetPaise){}
 public record CeremonyView(UUID id,UUID weddingId,String name,LocalDate eventDate,String venue){}
 private static final org.springframework.jdbc.core.RowMapper<WeddingView> WEDDING=(rs,n)->new WeddingView(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("city"),rs.getObject("event_date",LocalDate.class),rs.getLong("total_budget_paise"));
 private static final org.springframework.jdbc.core.RowMapper<CeremonyView> CEREMONY=(rs,n)->new CeremonyView(rs.getObject("id",UUID.class),rs.getObject("wedding_id",UUID.class),rs.getString("name"),rs.getObject("event_date",LocalDate.class),rs.getString("venue"));
 private void owner(UUID wedding){Integer count=db.queryForObject("SELECT COUNT(*) FROM weddings WHERE id=? AND owner_id=?",Integer.class,wedding,SecurityConfig.currentUser());if(count==null||count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
 public WeddingView create(@Valid @RequestBody WeddingInput in){UUID id=UUID.randomUUID();db.update("INSERT INTO weddings(id,owner_id,title,city,event_date,total_budget_paise) VALUES(?,?,?,?,?,?)",id,SecurityConfig.currentUser(),in.title().trim(),in.city().trim(),in.eventDate(),in.totalBudgetPaise());return new WeddingView(id,in.title().trim(),in.city().trim(),in.eventDate(),in.totalBudgetPaise());}
 @GetMapping public List<WeddingView> list(){return db.query("SELECT id,title,city,event_date,total_budget_paise FROM weddings WHERE owner_id=? ORDER BY event_date",WEDDING,SecurityConfig.currentUser());}
 @GetMapping("/{id}") public WeddingView get(@PathVariable UUID id){owner(id);return db.queryForObject("SELECT id,title,city,event_date,total_budget_paise FROM weddings WHERE id=?",WEDDING,id);}
 @PostMapping("/{id}/ceremonies") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public CeremonyView addCeremony(@PathVariable UUID id,@Valid @RequestBody CeremonyInput in){owner(id);UUID cid=UUID.randomUUID();db.update("INSERT INTO ceremonies(id,wedding_id,name,event_date,venue) VALUES(?,?,?,?,?)",cid,id,in.name().trim(),in.eventDate(),in.venue());return new CeremonyView(cid,id,in.name().trim(),in.eventDate(),in.venue());}
 @GetMapping("/{id}/ceremonies") public List<CeremonyView> ceremonies(@PathVariable UUID id){owner(id);return db.query("SELECT id,wedding_id,name,event_date,venue FROM ceremonies WHERE wedding_id=? ORDER BY event_date",CEREMONY,id);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void delete(@PathVariable UUID id){owner(id);db.update("DELETE FROM weddings WHERE id=?",id);}
}
