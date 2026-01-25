package com.carpool.group.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Partial update: fields left out (null) are not changed. */
public record UpdateGroupRequest(
        @Size(max = 80) String name,
        @Size(max = 500) String description,
        @Size(max = 64) String timezone,
        @DecimalMin("0") @DecimalMax("10") BigDecimal costPerKm,
        @Pattern(regexp = "[A-Z]{3}", message = "must be a three-letter currency code") String currency) {
}
