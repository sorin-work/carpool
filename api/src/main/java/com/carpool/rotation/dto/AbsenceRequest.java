package com.carpool.rotation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AbsenceRequest(@NotNull LocalDate startsOn, @NotNull LocalDate endsOn, @Size(max = 200) String note) {
}
