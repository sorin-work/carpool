package com.carpool.ride.dto;

import com.carpool.group.GroupMember;

public record DriverInfo(Long memberId, String displayName) {

    public static DriverInfo of(GroupMember member) {
        return member == null ? null : new DriverInfo(member.getId(), member.getDisplayName());
    }
}
