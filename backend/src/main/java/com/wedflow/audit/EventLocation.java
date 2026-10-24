package com.wedflow.audit;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record EventLocation(BigDecimal latitude,BigDecimal longitude,String source,BigDecimal accuracyMeters,OffsetDateTime recordedAt,String unavailableReason){
 public EventLocation{
  if((latitude==null)!=(longitude==null))throw new IllegalArgumentException("Latitude and longitude must be supplied together");
  if(latitude!=null&&(latitude.compareTo(BigDecimal.valueOf(-90))<0||latitude.compareTo(BigDecimal.valueOf(90))>0))throw new IllegalArgumentException("Latitude out of range");
  if(longitude!=null&&(longitude.compareTo(BigDecimal.valueOf(-180))<0||longitude.compareTo(BigDecimal.valueOf(180))>0))throw new IllegalArgumentException("Longitude out of range");
  if(accuracyMeters!=null&&accuracyMeters.signum()<0)throw new IllegalArgumentException("Accuracy must be nonnegative");
  if(source==null||source.isBlank())source="UNAVAILABLE";
  if(source.length()>30)throw new IllegalArgumentException("Location source too long");
  if(unavailableReason!=null&&unavailableReason.length()>80)throw new IllegalArgumentException("Reason too long");
 }
 public static EventLocation unavailable(String reason){return new EventLocation(null,null,"UNAVAILABLE",null,null,reason);}
}
