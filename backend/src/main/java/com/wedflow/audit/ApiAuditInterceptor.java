package com.wedflow.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Best-effort audit of mutating API requests.
 * Headers are explicitly CLIENT_REPORTED, not independently verified GPS.
 * A missing or invalid location never suppresses the event.
 */
@Component
public class ApiAuditInterceptor implements HandlerInterceptor {
  private static final Logger LOG=LoggerFactory.getLogger(ApiAuditInterceptor.class);
  private static final Set<String> METHODS=Set.of("POST","PUT","PATCH","DELETE");
  private final AuditEventService audit;
  public ApiAuditInterceptor(AuditEventService audit){this.audit=audit;}

  @Override public void afterCompletion(HttpServletRequest request,
      HttpServletResponse response,Object handler,Exception error){
    if(!request.getRequestURI().startsWith("/api/v1/") ||
       !METHODS.contains(request.getMethod()))return;
    String requestId=String.valueOf(request.getAttribute("requestId"));
    try{
      Authentication auth=SecurityContextHolder.getContext().getAuthentication();
      UUID actor=auth!=null && auth.getPrincipal() instanceof UUID id?id:null;
      Object rawPattern=request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      String pattern=rawPattern instanceof String ? (String)rawPattern : "/api/v1/unknown";
      String action=request.getMethod()+" "+pattern;
      if(action.length()>100)action=action.substring(0,100);
      UUID subject=subjectId(request);
      audit.record(actor,action,subject,clientLocation(request),response.getStatus(),
        requestId.length()>80?requestId.substring(0,80):requestId);
    }catch(Exception ex){
      LOG.error("API audit persistence failed requestId={} reasonType={}",
        requestId,ex.getClass().getSimpleName());
    }
  }
  static UUID subjectId(HttpServletRequest request){
    Object raw=request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    if(!(raw instanceof Map<?,?> variables))return null;
    for(String key: new String[]{"weddingId","id","bookingId","vendorId","paymentId","guestId","taskId"}){
      Object value=variables.get(key);
      if(value!=null)try{return UUID.fromString(value.toString());}
      catch(IllegalArgumentException ignored){return null;}
    }
    return null;
  }
  static EventLocation clientLocation(HttpServletRequest request){
    String lat=request.getHeader("X-Geo-Latitude");
    String lng=request.getHeader("X-Geo-Longitude");
    if((lat==null||lat.isBlank()) && (lng==null||lng.isBlank()))
      return EventLocation.unavailable("NOT_PROVIDED");
    try{
      BigDecimal accuracy=null;
      String accuracyRaw=request.getHeader("X-Geo-Accuracy-M");
      if(accuracyRaw!=null&&!accuracyRaw.isBlank())accuracy=new BigDecimal(accuracyRaw);
      return new EventLocation(new BigDecimal(lat),new BigDecimal(lng),
        "CLIENT_REPORTED",accuracy,OffsetDateTime.now(ZoneOffset.UTC),null);
    }catch(IllegalArgumentException | NullPointerException ex){
      return EventLocation.unavailable("INVALID_COORDINATES");
    }
  }

  @Configuration
  public static class Registration implements WebMvcConfigurer {
    private final ApiAuditInterceptor interceptor;
    public Registration(ApiAuditInterceptor interceptor){this.interceptor=interceptor;}
    @Override public void addInterceptors(InterceptorRegistry registry){
      registry.addInterceptor(interceptor).addPathPatterns("/api/v1/**");
    }
  }
}
