package com.carpool.group.dto;

import com.carpool.group.MemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull MemberRole role) {
}
