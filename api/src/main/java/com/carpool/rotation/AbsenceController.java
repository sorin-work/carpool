package com.carpool.rotation;

import com.carpool.rotation.dto.AbsenceRequest;
import com.carpool.rotation.dto.AbsenceResponse;
import com.carpool.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AbsenceController {

    private final AbsenceService absenceService;

    public AbsenceController(AbsenceService absenceService) {
        this.absenceService = absenceService;
    }

    @GetMapping("/groups/{groupId}/absences")
    public List<AbsenceResponse> upcoming(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        return absenceService.upcoming(groupId, auth.id());
    }

    @PostMapping("/groups/{groupId}/absences")
    @ResponseStatus(HttpStatus.CREATED)
    public AbsenceResponse add(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                               @Valid @RequestBody AbsenceRequest request) {
        return absenceService.add(groupId, auth.id(), request);
    }

    @DeleteMapping("/absences/{absenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser auth, @PathVariable Long absenceId) {
        absenceService.delete(absenceId, auth.id());
    }
}
