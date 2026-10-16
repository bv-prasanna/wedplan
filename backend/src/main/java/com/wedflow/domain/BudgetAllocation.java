package com.wedflow.domain;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
public final class BudgetAllocation {
 private BudgetAllocation(){}
 public static long allocatedPaise(long totalPaise, BigDecimal percent){ if(totalPaise<0) throw new IllegalArgumentException("Budget cannot be negative"); Objects.requireNonNull(percent,"percent"); if(percent.compareTo(BigDecimal.ZERO)<0||percent.compareTo(new BigDecimal("100"))>0) throw new IllegalArgumentException("Percentage must be 0..100"); return BigDecimal.valueOf(totalPaise).multiply(percent).divide(new BigDecimal("100"),0,RoundingMode.HALF_UP).longValueExact(); }
 public static long remainingPaise(long totalPaise,Map<String,Long> allocations){ if(totalPaise<0) throw new IllegalArgumentException("Budget cannot be negative"); Objects.requireNonNull(allocations); long used=0; for(Long amount:allocations.values()){if(amount==null||amount<0)throw new IllegalArgumentException("Invalid allocation");used=Math.addExact(used,amount);} return Math.subtractExact(totalPaise,used); }
}
