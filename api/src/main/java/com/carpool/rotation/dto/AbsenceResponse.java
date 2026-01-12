package com.carpool.rotation.dto;

import com.carpool.rotation.Absence;

import java.time.LocalDate;

public record AbsenceResponse(Long id, Long memberId, String displayName, LocalDate startsOn, LocalDate endsOn,
                              String note) {

    public static AbsenceResponse from(Absence absence) {
        return new AbsenceResponse(absence.getId(), absence.getMember().getId(), absence.getMember().getDisplayName(),
                absence.getStartsOn(), absence.getEndsOn(), absence.getNote());
    }
}
