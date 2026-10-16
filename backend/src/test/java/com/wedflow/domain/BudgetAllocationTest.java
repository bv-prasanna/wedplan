package com.wedflow.domain;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class BudgetAllocationTest {
 @Test void halfBudget(){assertEquals(5000,BudgetAllocation.allocatedPaise(10000,new BigDecimal("50")));}
 @Test void zeroPercent(){assertEquals(0,BudgetAllocation.allocatedPaise(10000,BigDecimal.ZERO));}
 @Test void fullPercent(){assertEquals(10000,BudgetAllocation.allocatedPaise(10000,new BigDecimal("100")));}
 @Test void roundsHalfUp(){assertEquals(1,BudgetAllocation.allocatedPaise(1,new BigDecimal("50")));}
 @Test void rejectsNegativeBudget(){assertThrows(IllegalArgumentException.class,()->BudgetAllocation.allocatedPaise(-1,BigDecimal.ONE));}
 @Test void rejectsNegativePercent(){assertThrows(IllegalArgumentException.class,()->BudgetAllocation.allocatedPaise(100,new BigDecimal("-1")));}
 @Test void rejectsAboveHundred(){assertThrows(IllegalArgumentException.class,()->BudgetAllocation.allocatedPaise(100,new BigDecimal("101")));}
 @Test void rejectsNullPercent(){assertThrows(NullPointerException.class,()->BudgetAllocation.allocatedPaise(100,null));}
 @Test void remaining(){assertEquals(200,BudgetAllocation.remainingPaise(1000,Map.of("a",300L,"b",500L)));}
 @Test void overspentIsNegative(){assertEquals(-100,BudgetAllocation.remainingPaise(100,Map.of("a",200L)));}
 @Test void rejectsNegativeAllocation(){assertThrows(IllegalArgumentException.class,()->BudgetAllocation.remainingPaise(100,Map.of("a",-1L)));}
 @Test void detectsOverflow(){assertThrows(ArithmeticException.class,()->BudgetAllocation.remainingPaise(Long.MAX_VALUE,Map.of("a",Long.MAX_VALUE,"b",1L)));}
}
