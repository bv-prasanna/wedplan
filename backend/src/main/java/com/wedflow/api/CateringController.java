package com.wedflow.api;
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
@RestController @RequestMapping("/api/v1/weddings/{weddingId}/catering")
public class CateringController {
 private final JdbcTemplate db;public CateringController(JdbcTemplate db){this.db=db;}
 private void owner(UUID wedding){Integer n=db.queryForObject("SELECT COUNT(*) FROM weddings WHERE id=? AND owner_id=?",Integer.class,wedding,SecurityConfig.currentUser());if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 public record MenuInput(UUID ceremonyId,@NotBlank @Size(max=30) String meal,@Positive int guestCount){}
 public record ItemInput(@NotBlank @Size(max=150) String name,@NotNull @Pattern(regexp="VEG|NON_VEG|JAIN") String diet,@Positive int quantity){}
 @PostMapping("/menus") @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,Object> menu(@PathVariable UUID weddingId,@Valid @RequestBody MenuInput in){owner(weddingId);if(in.ceremonyId()!=null){Integer n=db.queryForObject("SELECT COUNT(*) FROM ceremonies WHERE id=? AND wedding_id=?",Integer.class,in.ceremonyId(),weddingId);if(n==null||n==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid ceremony");}UUID id=UUID.randomUUID();db.update("INSERT INTO catering_menus(id,wedding_id,ceremony_id,meal,guest_count) VALUES(?,?,?,?,?)",id,weddingId,in.ceremonyId(),in.meal().trim(),in.guestCount());return Map.of("id",id,"guestCount",in.guestCount());}
 @GetMapping("/menus") public List<Map<String,Object>> menus(@PathVariable UUID weddingId){owner(weddingId);return db.query("SELECT id,meal,guest_count FROM catering_menus WHERE wedding_id=?",(rs,n)->Map.of("id",rs.getObject(1,UUID.class),"meal",rs.getString(2),"guestCount",rs.getInt(3)),weddingId);}
 @PostMapping("/menus/{menuId}/items") @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,Object> item(@PathVariable UUID weddingId,@PathVariable UUID menuId,@Valid @RequestBody ItemInput in){owner(weddingId);Integer n=db.queryForObject("SELECT COUNT(*) FROM catering_menus WHERE id=? AND wedding_id=?",Integer.class,menuId,weddingId);if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);UUID id=UUID.randomUUID();db.update("INSERT INTO catering_items(id,menu_id,name,diet,quantity) VALUES(?,?,?,?,?)",id,menuId,in.name().trim(),in.diet(),in.quantity());return Map.of("id",id,"diet",in.diet());}
 @GetMapping("/menus/{menuId}/items") public List<Map<String,Object>> items(@PathVariable UUID weddingId,@PathVariable UUID menuId){owner(weddingId);Integer n=db.queryForObject("SELECT COUNT(*) FROM catering_menus WHERE id=? AND wedding_id=?",Integer.class,menuId,weddingId);if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);return db.query("SELECT id,name,diet,quantity FROM catering_items WHERE menu_id=?",(rs,i)->Map.of("id",rs.getObject(1,UUID.class),"name",rs.getString(2),"diet",rs.getString(3),"quantity",rs.getInt(4)),menuId);}
}
