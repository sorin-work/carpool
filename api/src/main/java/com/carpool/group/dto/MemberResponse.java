package com.carpool.group.dto;

import com.carpool.group.GroupMember;
import com.carpool.group.MemberRole;

import java.time.Instant;

public record MemberResponse(Long id, Long userId, String displayName, MemberRole role, Instant joinedAt) {

    public static MemberResponse from(GroupMember member) {
        return new MemberResponse(member.getId(), member.getUser().getId(), member.getDisplayName(),
                member.getRole(), member.getJoinedAt());
    }
}
