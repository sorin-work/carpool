package com.carpool.rotation;

import com.carpool.rotation.dto.RotationProposal;
import com.carpool.security.AuthUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/rotation")
public class RotationController {

    private final RotationService rotationService;

    public RotationController(RotationService rotationService) {
        this.rotationService = rotationService;
    }

    @GetMapping
    public List<RotationProposal> preview(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return rotationService.preview(groupId, auth.id(), from, to);
    }

    @PostMapping("/apply")
    public List<RotationProposal> apply(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return rotationService.apply(groupId, auth.id(), from, to);
    }
}
