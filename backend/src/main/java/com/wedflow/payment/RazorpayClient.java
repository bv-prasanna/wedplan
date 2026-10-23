package com.wedflow.payment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
@Component public class RazorpayClient {
 private final String keyId,keySecret;private final ObjectMapper mapper;private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 public RazorpayClient(@Value("${RAZORPAY_KEY_ID:}")String keyId,@Value("${RAZORPAY_KEY_SECRET:}")String keySecret,ObjectMapper mapper){this.keyId=keyId;this.keySecret=keySecret;this.mapper=mapper;}
 public String keyId(){return keyId;}
 public String keySecret(){return keySecret;}
 public JsonNode post(String path,Map<String,Object> payload){return request("POST",path,payload);}
 public JsonNode get(String path){return request("GET",path,null);}
 private JsonNode request(String method,String path,Map<String,Object> payload){
  if(keyId.isBlank()||keySecret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Payment gateway not configured");
  if(!path.matches("/(orders|payments|refunds)(/[a-zA-Z0-9_]+)?(/refund)?"))throw new IllegalArgumentException("Invalid Razorpay API path");
  try{
   var builder=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1"+path)).timeout(Duration.ofSeconds(12)).header("Authorization","Basic "+Base64.getEncoder().encodeToString((keyId+":"+keySecret).getBytes(StandardCharsets.UTF_8))).header("Content-Type","application/json");
   var req=builder.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
   var response=client.send(req,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()<200||response.statusCode()>=300)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment provider request failed");
   return mapper.readTree(response.body());
  }catch(ResponseStatusException e){throw e;}catch(Exception e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment provider unavailable");}
 }
}
