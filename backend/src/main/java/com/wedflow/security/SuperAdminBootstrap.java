package com.wedflow.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Locale;
import java.util.UUID;

/**
 * Explicit one-time SUPER_ADMIN provisioning via deployment secret environment.
 * Does not change any existing admin password, nor allow public elevation.
 *
 * Set WEDFLOW_BOOTSTRAP_ADMIN_EMAIL/PASSWORD only on a controlled initial
 * deployment and remove both secret values immediately after provisioning.
 */
@Component
public class SuperAdminBootstrap implements ApplicationRunner {
 private static final Logger LOG=LoggerFactory.getLogger(SuperAdminBootstrap.class);
 private final JdbcTemplate db;
 private final PasswordEncoder encoder;
 private final PlatformTransactionManager transactions;
 private final String email,password;
 public SuperAdminBootstrap(JdbcTemplate db,PasswordEncoder encoder,
     PlatformTransactionManager transactions,
     @Value("${WEDFLOW_BOOTSTRAP_ADMIN_EMAIL:}") String email,
     @Value("${WEDFLOW_BOOTSTRAP_ADMIN_PASSWORD:}") String password){
   this.db=db;this.encoder=encoder;this.transactions=transactions;
   this.email=email;this.password=password;
 }
 @Override public void run(ApplicationArguments arguments) {
   if(email.isBlank()&&password.isBlank())return;
   if(email.isBlank()||password.isBlank()||email.length()>254||
      !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||
      password.length()<16||password.length()>128)
     throw new IllegalStateException("Invalid or incomplete initial admin bootstrap configuration");
   String canonical=email.trim().toLowerCase(Locale.ROOT);
   new TransactionTemplate(transactions).executeWithoutResult(tx->{
     // Serialize a first-deployment race between two independently starting services.
     db.execute("LOCK TABLE app_users IN EXCLUSIVE MODE");
     Integer admins=db.queryForObject(
       "SELECT COUNT(*) FROM app_users WHERE role IN ('SUPER_ADMIN','ADMIN')",Integer.class);
     if(admins!=null && admins>0){
       LOG.info("Admin bootstrap skipped: an administrator already exists");
       return;
     }
     Integer existing=db.queryForObject("SELECT COUNT(*) FROM app_users WHERE email=?",Integer.class,canonical);
     if(existing!=null && existing>0)
       throw new IllegalStateException("Bootstrap account already exists as non-admin; manual review required");
     db.update("INSERT INTO app_users(id,email,password_hash,role) VALUES(?,?,?,?)",
       UUID.randomUUID(),canonical,encoder.encode(password),"SUPER_ADMIN");
     LOG.warn("Initial super administrator created; remove bootstrap secrets from deployment settings");
   });
 }
}
