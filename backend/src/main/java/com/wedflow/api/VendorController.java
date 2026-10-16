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
import java.util.UUID;

@RestController @RequestMapping("/api/v1")
public class VendorController {
 private final JdbcTemplate db;public VendorController(JdbcTemplate db){this.db=db;}
 public record VendorView(UUID id,String displayName,String category,String city){}
 public record EnquiryInput(@NotNull UUID vendorId,@NotBlank @Size(max=4000) String message){}
 public record EnquiryView(UUID id,UUID weddingId,UUID vendorId,String message,String status){}
 private void owner(UUID wedding){Integer count=db.queryForObject("SELECT COUNT(*) FROM weddings WHERE id=? AND owner_id=?",Integer.class,wedding,SecurityConfig.currentUser());if(count==null||count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 @GetMapping("/vendors") public List<VendorView> search(@RequestParam(required=false) String category,@RequestParam(required=false) String city){
  return db.query("SELECT id,display_name,category,city FROM vendors WHERE approved=true AND (? IS NULL OR lower(category)=lower(?)) AND (? IS NULL OR lower(city)=lower(?)) ORDER BY display_name LIMIT 100",(rs,n)->new VendorView(rs.getObject("id",UUID.class),rs.getString("display_name"),rs.getString("category"),rs.getString("city")),category,category,city,city);
 }
 @PostMapping("/weddings/{id}/enquiries") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public EnquiryView enquire(@PathVariable UUID id,@Valid @RequestBody EnquiryInput in){
  owner(id);Integer count=db.queryForObject("SELECT COUNT(*) FROM vendors WHERE id=? AND approved=true",Integer.class,in.vendorId());if(count==null||count==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Vendor unavailable");
  UUID eid=UUID.randomUUID();db.update("INSERT INTO enquiries(id,wedding_id,vendor_id,message) VALUES(?,?,?,?)",eid,id,in.vendorId(),in.message().trim());return new EnquiryView(eid,id,in.vendorId(),in.message().trim(),"REQUESTED");
 }
 @GetMapping("/weddings/{id}/enquiries") public List<EnquiryView> enquiries(@PathVariable UUID id){
  owner(id);return db.query("SELECT id,wedding_id,vendor_id,message,status FROM enquiries WHERE wedding_id=? ORDER BY created_at DESC",(rs,n)->new EnquiryView(rs.getObject("id",UUID.class),rs.getObject("wedding_id",UUID.class),rs.getObject("vendor_id",UUID.class),rs.getString("message"),rs.getString("status")),id);
 }
}
