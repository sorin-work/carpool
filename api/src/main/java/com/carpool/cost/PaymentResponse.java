package com.carpool.cost;

import java.time.LocalDate;

public record PaymentResponse(Long id, Long fromMemberId, String fromName, Long toMemberId, String toName,
                              long amountCents, LocalDate paidOn) {

    static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getFrom().getId(), p.getFrom().getDisplayName(), p.getTo().getId(),
                p.getTo().getDisplayName(), p.getAmountCents(), p.getPaidOn());
    }
}
