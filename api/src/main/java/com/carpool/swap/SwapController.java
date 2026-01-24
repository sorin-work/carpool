package com.carpool.swap;

import com.carpool.security.AuthUser;
import com.carpool.swap.dto.CreateSwapRequest;
import com.carpool.swap.dto.SwapResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class SwapController {

    private final SwapService swapService;

    public SwapController(SwapService swapService) {
        this.swapService = swapService;
    }

    @PostMapping("/rides/{rideId}/swaps")
    @ResponseStatus(HttpStatus.CREATED)
    public SwapResponse create(@AuthenticationPrincipal AuthUser auth, @PathVariable Long rideId,
                               @Valid @RequestBody CreateSwapRequest request) {
        return swapService.create(rideId, auth.id(), request);
    }

    @GetMapping("/groups/{groupId}/swaps")
    public List<SwapResponse> list(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        return swapService.list(groupId, auth.id());
    }

    @PostMapping("/swaps/{swapId}/accept")
    public SwapResponse accept(@AuthenticationPrincipal AuthUser auth, @PathVariable Long swapId) {
        return swapService.accept(swapId, auth.id());
    }

    @PostMapping("/swaps/{swapId}/decline")
    public SwapResponse decline(@AuthenticationPrincipal AuthUser auth, @PathVariable Long swapId) {
        return swapService.decline(swapId, auth.id());
    }

    @PostMapping("/swaps/{swapId}/cancel")
    public SwapResponse cancel(@AuthenticationPrincipal AuthUser auth, @PathVariable Long swapId) {
        return swapService.cancel(swapId, auth.id());
    }
}
