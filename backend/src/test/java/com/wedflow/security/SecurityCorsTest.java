package com.wedflow.security;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import static org.junit.jupiter.api.Assertions.*;
class SecurityCorsTest {
 private final SecurityConfig config=new SecurityConfig();
 @Test void emptyOriginListDoesNotAllowBrowsers(){CorsConfiguration cors=config.corsConfigurationSource("").getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("GET","/api/v1/weddings"));assertNotNull(cors);assertNull(cors.checkOrigin("https://attacker.example"));}
 @Test void configuredOriginAllowed(){CorsConfiguration cors=config.corsConfigurationSource("https://wedflow.example").getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("GET","/api/v1/weddings"));assertEquals("https://wedflow.example",cors.checkOrigin("https://wedflow.example"));}
 @Test void unconfiguredOriginDenied(){CorsConfiguration cors=config.corsConfigurationSource("https://wedflow.example").getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("GET","/api/v1/weddings"));assertNull(cors.checkOrigin("https://attacker.example"));}
 @Test void preflightMethodsRestricted(){CorsConfiguration cors=config.corsConfigurationSource("https://wedflow.example").getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("OPTIONS","/api/v1/weddings"));assertNull(cors.checkHttpMethod(org.springframework.http.HttpMethod.TRACE));}
}
