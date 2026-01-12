package com.carpool.rotation.dto;

import java.time.Instant;
import java.time.LocalDate;

/** One ride and the member the rotation would put behind the wheel ({@code null} when nobody can). */
public record RotationProposal(Long rideId, String tripName, LocalDate date, Instant departureAt,
                               Long memberId, String displayName) {
}
