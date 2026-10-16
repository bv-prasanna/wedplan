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
import java.util.UUID;

@RestController @RequestMapping("/api/v1/weddings/{weddingId}")
public class PlanningController {
 private final JdbcTemplate db;public PlanningController(JdbcTemplate db){this.db=db;}
 private void owner(UUID id){Integer n=db.queryForObject("SELECT COUNT(*) FROM weddings WHERE id=? AND owner_id=?",Integer.class,id,SecurityConfig.currentUser());if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 public record BudgetInput(@NotBlank @Size(max=100) String name,@PositiveOrZero long allocatedPaise){}
 public record BudgetView(UUID id,String name,long allocatedPaise,long spentPaise){}
 @GetMapping("/budget") public List<BudgetView> budget(@PathVariable UUID weddingId){owner(weddingId);return db.query("SELECT id,name,allocated_paise,spent_paise FROM budget_categories WHERE wedding_id=? ORDER BY name",(rs,n)->new BudgetView(rs.getObject(1,UUID.class),rs.getString(2),rs.getLong(3),rs.getLong(4)),weddingId);}
 @PostMapping("/budget") @ResponseStatus(HttpStatus.CREATED) @Transactional public BudgetView addBudget(@PathVariable UUID weddingId,@Valid @RequestBody BudgetInput in){owner(weddingId);Long total=db.queryForObject("SELECT total_budget_paise FROM weddings WHERE id=? FOR UPDATE",Long.class,weddingId);Long used=db.queryForObject("SELECT COALESCE(SUM(allocated_paise),0) FROM budget_categories WHERE wedding_id=?",Long.class,weddingId);if(total==null||used==null||in.allocatedPaise()>total-used)throw new ResponseStatusException(HttpStatus.CONFLICT,"Budget allocation exceeds available balance");UUID id=UUID.randomUUID();db.update("INSERT INTO budget_categories(id,wedding_id,name,allocated_paise) VALUES(?,?,?,?)",id,weddingId,in.name().trim(),in.allocatedPaise());return new BudgetView(id,in.name().trim(),in.allocatedPaise(),0);}
 public record TaskInput(@NotBlank @Size(max=200) String title,LocalDate dueDate){}
 public record TaskView(UUID id,String title,LocalDate dueDate,boolean completed){}
 @GetMapping("/tasks") public List<TaskView> tasks(@PathVariable UUID weddingId){owner(weddingId);return db.query("SELECT id,title,due_date,completed FROM wedding_tasks WHERE wedding_id=? ORDER BY due_date NULLS LAST",(rs,n)->new TaskView(rs.getObject(1,UUID.class),rs.getString(2),rs.getObject(3,LocalDate.class),rs.getBoolean(4)),weddingId);}
 @PostMapping("/tasks") @ResponseStatus(HttpStatus.CREATED) @Transactional public TaskView addTask(@PathVariable UUID weddingId,@Valid @RequestBody TaskInput in){owner(weddingId);UUID id=UUID.randomUUID();db.update("INSERT INTO wedding_tasks(id,wedding_id,title,due_date) VALUES(?,?,?,?)",id,weddingId,in.title().trim(),in.dueDate());return new TaskView(id,in.title().trim(),in.dueDate(),false);}
 @PatchMapping("/tasks/{taskId}/complete") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void complete(@PathVariable UUID weddingId,@PathVariable UUID taskId){owner(weddingId);if(db.update("UPDATE wedding_tasks SET completed=true WHERE id=? AND wedding_id=?",taskId,weddingId)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 public record GuestInput(@NotBlank @Size(max=160) String fullName,@Size(max=30) String phone){}
 public record GuestView(UUID id,String fullName,String phone,String rsvp){}
 @GetMapping("/guests") public List<GuestView> guests(@PathVariable UUID weddingId){owner(weddingId);return db.query("SELECT id,full_name,phone,rsvp FROM wedding_guests WHERE wedding_id=? ORDER BY full_name",(rs,n)->new GuestView(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4)),weddingId);}
 @PostMapping("/guests") @ResponseStatus(HttpStatus.CREATED) @Transactional public GuestView addGuest(@PathVariable UUID weddingId,@Valid @RequestBody GuestInput in){owner(weddingId);UUID id=UUID.randomUUID();db.update("INSERT INTO wedding_guests(id,wedding_id,full_name,phone) VALUES(?,?,?,?)",id,weddingId,in.fullName().trim(),in.phone());return new GuestView(id,in.fullName().trim(),in.phone(),"PENDING");}
 @PatchMapping("/guests/{guestId}/rsvp") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void rsvp(@PathVariable UUID weddingId,@PathVariable UUID guestId,@RequestParam String status){owner(weddingId);if(!List.of("PENDING","ACCEPTED","DECLINED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);if(db.update("UPDATE wedding_guests SET rsvp=? WHERE id=? AND wedding_id=?",status,guestId,weddingId)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
}
