package com.carpool.cost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CostReport(LocalDate from, LocalDate to, String currency, BigDecimal costPerKm, long totalCents,
                         List<MemberBalance> balances) {
}
