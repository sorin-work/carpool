package com.carpool.trip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record TripRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 200) String origin,
        @NotBlank @Size(max = 200) String destination,
        @NotNull LocalTime departureTime,
        @NotEmpty Set<DayOfWeek> days,
        @NotNull @Positive @DecimalMax("2000") BigDecimal distanceKm,
        @NotNull LocalDate startsOn,
        LocalDate endsOn) {
}
