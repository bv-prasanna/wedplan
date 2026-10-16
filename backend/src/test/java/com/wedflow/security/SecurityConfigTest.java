package com.wedflow.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SecurityConfigTest {
 @Test void deterministicSha256(){assertEquals(SecurityConfig.sha256("token"),SecurityConfig.sha256("token"));}
 @Test void distinctTokensHaveDifferentHashes(){assertNotEquals(SecurityConfig.sha256("a"),SecurityConfig.sha256("b"));}
 @Test void sha256Has64HexChars(){assertTrue(SecurityConfig.sha256("hello").matches("[0-9a-f]{64}"));}
 @Test void noAuthenticatedUserIsRejected(){org.springframework.security.core.context.SecurityContextHolder.clearContext();assertThrows(org.springframework.web.server.ResponseStatusException.class,SecurityConfig::currentUser);}
}
