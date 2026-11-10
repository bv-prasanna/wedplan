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
  // Build parameterized predicates only when supplied. Binding NULL to
  // "? IS NULL" can be ambiguous in PostgreSQL and breaks unfiltered search.
  StringBuilder sql=new StringBuilder("SELECT id,display_name,category,city FROM vendors WHERE approved=true");
  java.util.List<Object> args=new java.util.ArrayList<>();
  if(category!=null&&!category.isBlank()){
    String value=category.trim();
    if(value.length()>80)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Category filter too long");
    sql.append(" AND POSITION(lower(?) IN lower(category))>0");
    args.add(value);
  }
  if(city!=null&&!city.isBlank()){
    String value=city.trim();
    if(value.length()>120)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"City filter too long");
    sql.append(" AND POSITION(lower(?) IN lower(city))>0");
    args.add(value);
  }
  sql.append(" ORDER BY display_name LIMIT 100");
  return db.query(sql.toString(),
    (rs,n)->new VendorView(rs.getObject("id",UUID.class),rs.getString("display_name"),
      rs.getString("category"),rs.getString("city")),args.toArray());
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
