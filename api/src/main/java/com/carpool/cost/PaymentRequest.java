package com.carpool.cost;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** {@code paidOn} defaults to today. */
public record PaymentRequest(@NotNull Long fromMemberId, @NotNull Long toMemberId,
                             @Positive @Max(10_000_000) long amountCents, LocalDate paidOn) {
}
