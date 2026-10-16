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
public class CommerceController {
 private final JdbcTemplate db;public CommerceController(JdbcTemplate db){this.db=db;}
 private void owner(UUID wedding){Integer n=db.queryForObject("SELECT COUNT(*) FROM weddings WHERE id=? AND owner_id=?",Integer.class,wedding,SecurityConfig.currentUser());if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 private void vendorOwner(UUID vendor){Integer n=db.queryForObject("SELECT COUNT(*) FROM vendor_profiles WHERE id=? AND owner_id=?",Integer.class,vendor,SecurityConfig.currentUser());if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 public record VendorRegistration(@NotBlank @Size(max=200) String displayName,@NotBlank @Size(max=80) String category,@NotBlank @Size(max=120) String city,@Size(max=2000) String description){}
 @PostMapping("/vendor-profiles") @ResponseStatus(HttpStatus.CREATED) @Transactional public java.util.Map<String,Object> registerVendor(@Valid @RequestBody VendorRegistration in){
  UUID id=UUID.randomUUID();db.update("INSERT INTO vendors(id,display_name,category,city,approved) VALUES(?,?,?,?,false)",id,in.displayName().trim(),in.category().trim(),in.city().trim());db.update("INSERT INTO vendor_profiles(id,owner_id,description) VALUES(?,?,?)",id,SecurityConfig.currentUser(),in.description());return java.util.Map.of("id",id,"status","PENDING");
 }
 public record ServiceInput(@NotBlank @Size(max=200) String title,@PositiveOrZero long basePricePaise){}
 @PostMapping("/vendor-profiles/{vendorId}/services") @ResponseStatus(HttpStatus.CREATED) @Transactional public java.util.Map<String,Object> service(@PathVariable UUID vendorId,@Valid @RequestBody ServiceInput in){
  vendorOwner(vendorId);UUID id=UUID.randomUUID();db.update("INSERT INTO vendor_services(id,vendor_id,title,base_price_paise) VALUES(?,?,?,?)",id,vendorId,in.title().trim(),in.basePricePaise());return java.util.Map.of("id",id,"vendorId",vendorId);
 }
 public record QuoteInput(@NotNull UUID enquiryId,@Positive long amountPaise,@Size(max=2000) String description){}
 @PostMapping("/vendor-profiles/{vendorId}/quotes") @ResponseStatus(HttpStatus.CREATED) @Transactional public java.util.Map<String,Object> quote(@PathVariable UUID vendorId,@Valid @RequestBody QuoteInput in){
  vendorOwner(vendorId);Integer n=db.queryForObject("SELECT COUNT(*) FROM enquiries WHERE id=? AND vendor_id=?",Integer.class,in.enquiryId(),vendorId);if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);UUID id=UUID.randomUUID();db.update("INSERT INTO vendor_quotes(id,enquiry_id,vendor_id,amount_paise,description) VALUES(?,?,?,?,?)",id,in.enquiryId(),vendorId,in.amountPaise(),in.description());return java.util.Map.of("id",id,"status","OFFERED");
 }
 @GetMapping("/weddings/{weddingId}/quotes") public List<java.util.Map<String,Object>> quotes(@PathVariable UUID weddingId){owner(weddingId);return db.query("SELECT q.id,q.vendor_id,q.amount_paise,q.status FROM vendor_quotes q JOIN enquiries e ON q.enquiry_id=e.id WHERE e.wedding_id=?",(rs,n)->java.util.Map.of("id",rs.getObject(1,UUID.class),"vendorId",rs.getObject(2,UUID.class),"amountPaise",rs.getLong(3),"status",rs.getString(4)),weddingId);}
 @PostMapping("/weddings/{weddingId}/quotes/{quoteId}/accept") @ResponseStatus(HttpStatus.CREATED) @Transactional public java.util.Map<String,Object> accept(@PathVariable UUID weddingId,@PathVariable UUID quoteId){
  owner(weddingId);List<UUID> vendors=db.query("SELECT q.vendor_id FROM vendor_quotes q JOIN enquiries e ON e.id=q.enquiry_id WHERE q.id=? AND e.wedding_id=? AND q.status='OFFERED' FOR UPDATE OF q",(rs,n)->rs.getObject(1,UUID.class),quoteId,weddingId);if(vendors.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Quote not available");UUID id=UUID.randomUUID();db.update("UPDATE vendor_quotes SET status='ACCEPTED' WHERE id=?",quoteId);db.update("INSERT INTO vendor_bookings(id,quote_id,wedding_id,vendor_id) VALUES(?,?,?,?)",id,quoteId,weddingId,vendors.get(0));return java.util.Map.of("id",id,"status","CONFIRMED");
 }
 @GetMapping("/weddings/{weddingId}/bookings") public List<java.util.Map<String,Object>> bookings(@PathVariable UUID weddingId){owner(weddingId);return db.query("SELECT id,vendor_id,status FROM vendor_bookings WHERE wedding_id=?",(rs,n)->java.util.Map.of("id",rs.getObject(1,UUID.class),"vendorId",rs.getObject(2,UUID.class),"status",rs.getString(3)),weddingId);}
 @PostMapping("/weddings/{weddingId}/bookings/{bookingId}/cancel") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void cancel(@PathVariable UUID weddingId,@PathVariable UUID bookingId){owner(weddingId);if(db.update("UPDATE vendor_bookings SET status='CANCELLED' WHERE id=? AND wedding_id=? AND status='CONFIRMED'",bookingId,weddingId)==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Booking cannot be cancelled");}
}
