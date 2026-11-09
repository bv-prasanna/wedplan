package com.wedflow.api;

import com.wedflow.security.SecurityConfig;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Aggregated operational KPIs, deliberately excluding customer PII. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminDashboardController {
  private final JdbcTemplate db;
  public AdminDashboardController(JdbcTemplate db){this.db=db;}

  private void admin() {
    List<String> roles=db.query("SELECT role FROM app_users WHERE id=?",
      (rs,n)->rs.getString(1),SecurityConfig.currentUser());
    if(roles.isEmpty()||(!"ADMIN".equals(roles.get(0))&&
      !"SUPER_ADMIN".equals(roles.get(0))))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  public record Summary(long users,long weddings,long approvedVendors,long pendingVendors,
                        long confirmedBookings,long capturedPaise,long processedRefundPaise) {}

  private long number(String sql){
    Long n=db.queryForObject(sql,Long.class);
    return n==null?0:n;
  }

  @GetMapping("/summary")
  public Summary summary() {
    admin();
    return new Summary(
      number("SELECT COUNT(*) FROM app_users"),
      number("SELECT COUNT(*) FROM weddings"),
      number("SELECT COUNT(*) FROM vendor_profiles WHERE verification_status='APPROVED'"),
      number("SELECT COUNT(*) FROM vendor_profiles WHERE verification_status='PENDING'"),
      number("SELECT COUNT(*) FROM vendor_bookings WHERE status='CONFIRMED'"),
      number("SELECT COALESCE(SUM(amount_paise),0) FROM payment_records WHERE status='VERIFIED'"),
      number("SELECT COALESCE(SUM(amount_paise),0) FROM payment_refunds WHERE status='PROCESSED'")
    );
  }
}
