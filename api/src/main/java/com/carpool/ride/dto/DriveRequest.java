package com.carpool.ride.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** {@code seats} defaults to every seat in the driver's car. */
public record DriveRequest(@Min(1) @Max(8) Integer seats) {
}
