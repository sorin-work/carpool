package com.carpool.cost;

import com.carpool.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class CostController {

    private final CostService costService;

    public CostController(CostService costService) {
        this.costService = costService;
    }

    @GetMapping("/api/groups/{groupId}/costs")
    public CostReport costs(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return costService.report(groupId, auth.id(), from, to);
    }

    @PostMapping("/api/groups/{groupId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse record(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                                  @Valid @RequestBody PaymentRequest request) {
        return costService.record(groupId, auth.id(), request);
    }
}
