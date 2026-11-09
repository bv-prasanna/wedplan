package com.wedflow.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Logs operational HTTP metadata only. Query strings, payloads, tokens,
 * user identity and geolocation headers must never appear in this log.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class ApiTelemetryFilter extends OncePerRequestFilter {
  private static final Logger LOG = LoggerFactory.getLogger(ApiTelemetryFilter.class);

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    long start = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
      String path = request.getRequestURI()
        .replaceAll("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", ":id");
      if(path.length()>200) path=path.substring(0,200);
      LOG.info("api requestId={} method={} route={} status={} durationMs={}",
        response.getHeader("X-Request-ID"),request.getMethod(),path,response.getStatus(),durationMs);
    }
  }
}
