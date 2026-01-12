package com.carpool.rotation;

import com.carpool.common.ApiException;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.rotation.dto.AbsenceRequest;
import com.carpool.rotation.dto.AbsenceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AbsenceService {

    static final int MAX_DAYS = 120;

    private final AbsenceRepository absences;
    private final GroupAccess access;
    private final Clock clock;

    public AbsenceService(AbsenceRepository absences, GroupAccess access, Clock clock) {
        this.absences = absences;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AbsenceResponse> upcoming(Long groupId, Long userId) {
        GroupMember me = access.requireMember(groupId, userId);
        LocalDate today = LocalDate.now(clock.withZone(me.getGroup().getZone()));
        return absences.findOverlapping(groupId, today, today.plusYears(1)).stream()
                .map(AbsenceResponse::from).toList();
    }

    @Transactional
    public AbsenceResponse add(Long groupId, Long userId, AbsenceRequest request) {
        GroupMember me = access.requireMember(groupId, userId);
        if (request.endsOn().isBefore(request.startsOn())) {
            throw ApiException.badRequest("The last day cannot be before the first");
        }
        if (ChronoUnit.DAYS.between(request.startsOn(), request.endsOn()) >= MAX_DAYS) {
            throw ApiException.badRequest("An absence can be at most " + MAX_DAYS + " days");
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        return AbsenceResponse.from(absences.save(new Absence(me, request.startsOn(), request.endsOn(), note,
                clock.instant())));
    }

    @Transactional
    public void delete(Long absenceId, Long userId) {
        Absence absence = absences.findById(absenceId).orElseThrow(() -> ApiException.notFound("Absence"));
        GroupMember me = access.requireMember(absence.getMember().getGroup().getId(), userId);
        if (!absence.getMember().getId().equals(me.getId()) && !me.getRole().canManage()) {
            throw ApiException.forbidden("You can only remove your own absences");
        }
        absences.delete(absence);
    }
}
