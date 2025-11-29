package com.carpool.group.dto;

import com.carpool.group.MemberRole;

public record GroupSummary(Long id, String name, String description, MemberRole myRole, long memberCount) {
}
