package com.carpool.stats;

import java.time.LocalDate;
import java.util.List;

public record GroupStats(LocalDate from, LocalDate to, int rides, int cancelled, List<MemberStats> members) {
}
