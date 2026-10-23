package com.wedflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Configuration
public class SecurityConfig {
  @Bean CorsConfigurationSource corsConfigurationSource(@Value("${WEDFLOW_ALLOWED_ORIGINS:}") String origins) {
    CorsConfiguration config=new CorsConfiguration();
    if(!origins.isBlank()) config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(v->!v.isEmpty()).toList());
    config.setAllowedMethods(List.of("GET","POST","PATCH","PUT","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization","Content-Type"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**",config);
    return source;
  }
  @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
  @Bean SecurityFilterChain security(HttpSecurity http, BearerFilter bearer) throws Exception {
    return http.csrf(csrf->csrf.disable()).cors(cors->{}).headers(headers->headers.contentTypeOptions(opts->{}).frameOptions(frame->frame.deny()))
      .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/register","/api/v1/auth/login","/api/v1/vendors","/actuator/health","/api/v1/razorpay/webhook").permitAll().anyRequest().authenticated())
      .addFilterBefore(bearer,UsernamePasswordAuthenticationFilter.class).build();
  }
  public static UUID currentUser(){
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if(auth==null || !(auth.getPrincipal() instanceof UUID id)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return id;
  }
  public static String sha256(String raw){
    try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}
    catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  }
  @Component public static class BearerFilter extends OncePerRequestFilter {
    private final JdbcTemplate db;
    public BearerFilter(JdbcTemplate db){this.db=db;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException {
      String value=req.getHeader("Authorization");
      if(value!=null && value.startsWith("Bearer ")){
        String token=value.substring(7);
        if(token.matches("[0-9a-f]{64}")){
          List<UUID> ids=db.query("SELECT user_id FROM auth_tokens WHERE token_hash=? AND expires_at > CURRENT_TIMESTAMP", (rs,n)->rs.getObject(1,UUID.class),sha256(token));
          if(!ids.isEmpty()) SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ids.get(0),null,List.of()));
        }
      }
      try{chain.doFilter(req,res);}finally{SecurityContextHolder.clearContext();}
    }
  }
}
