package com.carpool.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String description,
        @Size(max = 64) String timezone) {
}
