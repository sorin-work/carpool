package com.carpool.group.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateMembershipRequest(
        boolean canDrive,
        @Min(0) @Max(8) int carSeats,
        @Size(max = 200) String pickupNote) {
}
