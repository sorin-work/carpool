package com.carpool.swap.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSwapRequest(@NotNull Long targetMemberId, Long offeredRideId, @Size(max = 300) String message) {
}
