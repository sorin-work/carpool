package com.carpool.trip;

import com.carpool.security.AuthUser;
import com.carpool.trip.dto.CancelRangeRequest;
import com.carpool.trip.dto.ReturnTripRequest;
import com.carpool.trip.dto.TripRequest;
import com.carpool.trip.dto.TripResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @PutMapping("/trips/{tripId}")
    public TripResponse update(@AuthenticationPrincipal AuthUser auth, @PathVariable Long tripId,
                               @Valid @RequestBody TripRequest request) {
        return tripService.update(tripId, auth.id(), request);
    }

    @PostMapping("/trips/{tripId}/pause")
    public TripResponse pause(@AuthenticationPrincipal AuthUser auth, @PathVariable Long tripId) {
        return tripService.setActive(tripId, auth.id(), false);
    }

    @PostMapping("/trips/{tripId}/resume")
    public TripResponse resume(@AuthenticationPrincipal AuthUser auth, @PathVariable Long tripId) {
        return tripService.setActive(tripId, auth.id(), true);
    }

    @DeleteMapping("/trips/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser auth, @PathVariable Long tripId) {
        tripService.delete(tripId, auth.id());
    }

    @PostMapping("/trips/{tripId}/cancel-days")
    public Map<String, Integer> cancelDays(@AuthenticationPrincipal AuthUser auth, @PathVariable Long tripId,
                                           @Valid @RequestBody CancelRangeRequest request) {
        return Map.of("cancelled", tripService.cancelRange(tripId, auth.id(), request));
    }

    @PostMapping("/trips/{tripId}/return")
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse addReturn(@AuthenticationPrincipal AuthUser auth, @PathVariable Long tripId,
                                  @Valid @RequestBody ReturnTripRequest request) {
        return tripService.addReturn(tripId, auth.id(), request);
    }
}
