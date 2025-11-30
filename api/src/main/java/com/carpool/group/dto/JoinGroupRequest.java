package com.carpool.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinGroupRequest(@NotBlank @Size(max = 20) String inviteCode) {
}
