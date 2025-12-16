package com.carpool.ride;

import com.carpool.ride.dto.DriveRequest;
import com.carpool.ride.dto.RideResponse;
import com.carpool.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/groups/{groupId}/rides")
    public List<RideResponse> schedule(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return rideService.schedule(groupId, auth.id(), from, to);
    }

    @GetMapping("/rides/{rideId}")
    public RideResponse get(@AuthenticationPrincipal AuthUser auth, @PathVariable Long rideId) {
        return rideService.get(rideId, auth.id());
    }

    @PostMapping("/rides/{rideId}/driver")
    public RideResponse drive(@AuthenticationPrincipal AuthUser auth, @PathVariable Long rideId,
                              @Valid @RequestBody(required = false) DriveRequest request) {
        return rideService.drive(rideId, auth.id(), request != null ? request.seats() : null);
    }

    @DeleteMapping("/rides/{rideId}/driver")
    public RideResponse stopDriving(@AuthenticationPrincipal AuthUser auth, @PathVariable Long rideId) {
        return rideService.stopDriving(rideId, auth.id());
    }
}
