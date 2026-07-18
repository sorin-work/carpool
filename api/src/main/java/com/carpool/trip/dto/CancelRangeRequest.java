package com.carpool.trip.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Both days are inclusive, like a school holiday from Monday to Friday. */
public record CancelRangeRequest(@NotNull LocalDate from, @NotNull LocalDate to, @Size(max = 300) String reason) {
}
