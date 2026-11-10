package com.wedflow.security;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuperAdminBootstrapTest {
 private final JdbcTemplate db=mock(JdbcTemplate.class);
 private final PasswordEncoder encoder=mock(PasswordEncoder.class);
 private final PlatformTransactionManager transactionManager=mock(PlatformTransactionManager.class);

 @Test void noBootstrapSecretsMeansNoDatabaseMutation(){
  new SuperAdminBootstrap(db,encoder,transactionManager,"","").run(null);
  verifyNoInteractions(db,encoder,transactionManager);
 }
 @Test void partialOrWeakCredentialsFailClosedWithoutLoggingSecretValues(){
  var onlyEmail=new SuperAdminBootstrap(db,encoder,transactionManager,
    "admin@example.com","");
  assertThrows(IllegalStateException.class,()->onlyEmail.run(null));
  var weak=new SuperAdminBootstrap(db,encoder,transactionManager,
    "admin@example.com","short");
  assertThrows(IllegalStateException.class,()->weak.run(null));
  var invalidEmail=new SuperAdminBootstrap(db,encoder,transactionManager,
    "admin", "AlongUniquePassword2026!");
  assertThrows(IllegalStateException.class,()->invalidEmail.run(null));
  verifyNoInteractions(db,encoder,transactionManager);
 }
}
