package com.carpool.group.dto;

import jakarta.validation.constraints.Size;

/** Partial update: fields left out (null) are not changed. */
public record UpdateGroupRequest(
        @Size(max = 80) String name,
        @Size(max = 500) String description,
        @Size(max = 64) String timezone) {
}
