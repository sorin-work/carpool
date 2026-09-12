package com.carpool.trip.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record ReturnTripRequest(@NotNull LocalTime departureTime, @Size(max = 80) String name) {
}
