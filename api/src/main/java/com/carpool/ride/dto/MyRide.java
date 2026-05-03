package com.carpool.ride.dto;

/** One of my upcoming rides, with the group it belongs to and whether I drive it. */
public record MyRide(Long groupId, String groupName, String timezone, boolean driving, RideResponse ride) {
}
