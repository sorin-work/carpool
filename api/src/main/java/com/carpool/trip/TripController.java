package com.carpool.trip;

import com.carpool.security.AuthUser;
import com.carpool.trip.dto.TripRequest;
import com.carpool.trip.dto.TripResponse;
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
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/groups/{groupId}/trips")
    public List<TripResponse> list(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        return tripService.list(groupId, auth.id());
    }

    @PostMapping("/groups/{groupId}/trips")
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                               @Valid @RequestBody TripRequest request) {
        return tripService.create(groupId, auth.id(), request);
    }

    @GetMapping("/trips/{tripId}")
    public TripResponse get(@AuthenticationPrincipal AuthUser auth, @PathVariable Long tripId) {
        return tripService.get(tripId, auth.id());
    }
}
