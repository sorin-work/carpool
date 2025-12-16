package com.carpool.booking;

import com.carpool.booking.dto.BookingResponse;
import com.carpool.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rides/{rideId}")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/passengers")
    public List<BookingResponse> passengers(@AuthenticationPrincipal AuthUser auth, @PathVariable Long rideId) {
        return bookingService.passengers(rideId, auth.id());
    }

    @PostMapping("/seat")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse request(@AuthenticationPrincipal AuthUser auth, @PathVariable Long rideId) {
        return bookingService.request(rideId, auth.id());
    }

    @DeleteMapping("/seat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthUser auth, @PathVariable Long rideId) {
        bookingService.cancel(rideId, auth.id());
    }
}
