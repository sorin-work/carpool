package com.carpool.group.dto;

import com.carpool.group.MemberRole;

import java.util.List;

public record GroupDetail(Long id, String name, String description, String timezone, String inviteCode,
                          MemberRole myRole, Long myMemberId, List<MemberResponse> members) {
}
