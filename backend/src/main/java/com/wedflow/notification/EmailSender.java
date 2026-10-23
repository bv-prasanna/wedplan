package com.wedflow.notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
@Component public class EmailSender {
 private final String apiKey,from;
 public EmailSender(@Value("${BREVO_API_KEY:}")String apiKey,@Value("${NOTIFY_FROM_EMAIL:}")String from){this.apiKey=apiKey;this.from=from;}
 public void send(String recipient,String subject,String body)throws Exception {
  if(apiKey.isBlank()||from.isBlank())throw new IllegalStateException("Email delivery not configured");
  com.fasterxml.jackson.databind.ObjectMapper mapper=new com.fasterxml.jackson.databind.ObjectMapper();
  String payload=mapper.writeValueAsString(java.util.Map.of("sender",java.util.Map.of("email",from),"to",java.util.List.of(java.util.Map.of("email",recipient)),"subject",subject,"textContent",body));
  HttpRequest req=HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email")).timeout(Duration.ofSeconds(15)).header("api-key",apiKey).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(payload,StandardCharsets.UTF_8)).build();
  HttpResponse<String> res=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.ofString());
  if(res.statusCode()<200||res.statusCode()>=300)throw new IllegalStateException("Email provider returned HTTP "+res.statusCode());
 }
}
