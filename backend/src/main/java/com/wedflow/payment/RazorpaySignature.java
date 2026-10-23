package com.wedflow.payment;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
public final class RazorpaySignature {
 private RazorpaySignature(){}
 public static String hmac(String secret,String message){
  try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));}
  catch(Exception e){throw new IllegalStateException("Cannot compute signature",e);}
 }
 public static boolean verify(String secret,String message,String received){
  if(secret==null||secret.isBlank()||received==null||!received.matches("[a-fA-F0-9]{64}"))return false;
  return MessageDigest.isEqual(HexFormat.of().parseHex(hmac(secret,message)),HexFormat.of().parseHex(received));
 }
 public static boolean checkout(String secret,String storedOrderId,String paymentId,String signature){
  return verify(secret,storedOrderId+"|"+paymentId,signature);
 }
}
