package com.carpool.ride.dto;

import jakarta.validation.constraints.Size;

public record CancelRideRequest(@Size(max = 300) String reason) {
}
