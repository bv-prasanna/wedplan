package com.wedflow.audit;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class EventLocationTest {
 @Test void acceptsValidCoordinates(){var loc=new EventLocation(new BigDecimal("12.9716000"),new BigDecimal("77.5946000"),"DEVICE_GPS",BigDecimal.TEN,null,null);assertEquals("DEVICE_GPS",loc.source());}
 @Test void rejectsMissingLongitude(){assertThrows(IllegalArgumentException.class,()->new EventLocation(BigDecimal.ONE,null,"DEVICE_GPS",null,null,null));}
 @Test void rejectsOutOfRangeLatitude(){assertThrows(IllegalArgumentException.class,()->new EventLocation(new BigDecimal("91"),BigDecimal.ONE,"DEVICE_GPS",null,null,null));}
 @Test void rejectsOutOfRangeLongitude(){assertThrows(IllegalArgumentException.class,()->new EventLocation(BigDecimal.ONE,new BigDecimal("181"),"DEVICE_GPS",null,null,null));}
 @Test void unavailableIsExplicit(){var loc=EventLocation.unavailable("PERMISSION_DENIED");assertNull(loc.latitude());assertEquals("PERMISSION_DENIED",loc.unavailableReason());}
}
