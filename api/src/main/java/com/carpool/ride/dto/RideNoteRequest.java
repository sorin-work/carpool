package com.carpool.ride.dto;

import jakarta.validation.constraints.Size;

public record RideNoteRequest(@Size(max = 300) String note) {
}
