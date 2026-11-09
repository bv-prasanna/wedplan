package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read-only vendor/admin work queues. Ownership is always enforced in SQL. */
@RestController
@RequestMapping("/api/v1")
public class VendorWorkspaceController {
  private final JdbcTemplate db;
  public VendorWorkspaceController(JdbcTemplate db) { this.db = db; }

  public record VendorProfile(UUID id, String displayName, String category, String city,
                              String description, String status) {}
  public record ServiceView(UUID id, String title, long basePricePaise, boolean active) {}
  public record VendorEnquiry(UUID id, UUID weddingId, String weddingTitle, String message,
                              String status) {}
  public record VendorQuote(UUID id, UUID enquiryId, long amountPaise, String description,
                            String status) {}
  public record VendorBooking(UUID id, UUID weddingId, LocalDate serviceDate, String status) {}
  public record PendingVendor(UUID id, String displayName, String category, String city,
                              String description, String status) {}

  private void vendorOwner(UUID vendorId) {
    Integer n = db.queryForObject(
      "SELECT COUNT(*) FROM vendor_profiles WHERE id=? AND owner_id=?",
      Integer.class, vendorId, SecurityConfig.currentUser());
    if(n == null || n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }
  private void admin() {
    List<String> roles = db.query("SELECT role FROM app_users WHERE id=?",
      (rs, i) -> rs.getString(1), SecurityConfig.currentUser());
    if(roles.isEmpty() || (!"ADMIN".equals(roles.get(0)) &&
        !"SUPER_ADMIN".equals(roles.get(0)))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @GetMapping("/vendor-profiles/mine")
  public List<VendorProfile> mine() {
    return db.query("""
      SELECT v.id,v.display_name,v.category,v.city,p.description,p.verification_status
      FROM vendor_profiles p JOIN vendors v ON v.id=p.id
      WHERE p.owner_id=? ORDER BY v.display_name
      """, (rs,i) -> new VendorProfile(rs.getObject(1,UUID.class),rs.getString(2),
      rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)),
      SecurityConfig.currentUser());
  }
  @GetMapping("/vendor-profiles/{vendorId}/services")
  public List<ServiceView> services(@PathVariable UUID vendorId) {
    vendorOwner(vendorId);
    return db.query("""
      SELECT id,title,base_price_paise,active FROM vendor_services
      WHERE vendor_id=? ORDER BY title
      """, (rs,i) -> new ServiceView(rs.getObject(1,UUID.class),rs.getString(2),
      rs.getLong(3),rs.getBoolean(4)), vendorId);
  }
  @GetMapping("/vendor-profiles/{vendorId}/enquiries")
  public List<VendorEnquiry> enquiries(@PathVariable UUID vendorId) {
    vendorOwner(vendorId);
    return db.query("""
      SELECT e.id,e.wedding_id,w.title,e.message,e.status
      FROM enquiries e JOIN weddings w ON w.id=e.wedding_id
      WHERE e.vendor_id=? ORDER BY e.created_at DESC LIMIT 200
      """, (rs,i) -> new VendorEnquiry(rs.getObject(1,UUID.class),
      rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getString(5)),vendorId);
  }
  @GetMapping("/vendor-profiles/{vendorId}/quotes")
  public List<VendorQuote> quotes(@PathVariable UUID vendorId) {
    vendorOwner(vendorId);
    return db.query("""
      SELECT id,enquiry_id,amount_paise,description,status
      FROM vendor_quotes WHERE vendor_id=? ORDER BY created_at DESC LIMIT 200
      """, (rs,i) -> new VendorQuote(rs.getObject(1,UUID.class),
      rs.getObject(2,UUID.class),rs.getLong(3),rs.getString(4),rs.getString(5)),vendorId);
  }
  @GetMapping("/vendor-profiles/{vendorId}/bookings")
  public List<VendorBooking> bookings(@PathVariable UUID vendorId) {
    vendorOwner(vendorId);
    return db.query("""
      SELECT id,wedding_id,service_date,status FROM vendor_bookings
      WHERE vendor_id=? ORDER BY created_at DESC LIMIT 200
      """,(rs,i)->new VendorBooking(rs.getObject(1,UUID.class),
      rs.getObject(2,UUID.class),rs.getObject(3,LocalDate.class),rs.getString(4)),vendorId);
  }
  @GetMapping("/admin/vendors/pending")
  public List<PendingVendor> pending() {
    admin();
    return db.query("""
      SELECT v.id,v.display_name,v.category,v.city,p.description,p.verification_status
      FROM vendor_profiles p JOIN vendors v ON v.id=p.id
      WHERE p.verification_status='PENDING'
      ORDER BY v.display_name LIMIT 200
      """,(rs,i)->new PendingVendor(rs.getObject(1,UUID.class),rs.getString(2),
      rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)));
  }
}
