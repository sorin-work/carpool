package com.carpool.booking;

import com.carpool.group.GroupMember;
import com.carpool.notification.NotificationService;
import com.carpool.notification.NotificationType;
import com.carpool.ride.Ride;
import com.carpool.ride.RideMessages;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * Re-runs seat allocation whenever a ride's driver, seats or bookings change, and tells riders who
 * just got a seat.
 */
@Component
public class SeatPlanner {

    private final SeatBookingRepository bookings;
    private final NotificationService notifications;
    private final Clock clock;

    public SeatPlanner(SeatBookingRepository bookings, NotificationService notifications, Clock clock) {
        this.bookings = bookings;
        this.notifications = notifications;
        this.clock = clock;
    }

    /** {@code actor} made the change and is not notified about it. */
    public List<SeatBooking> rebalance(Ride ride, GroupMember actor) {
        List<SeatBooking> active = bookings.findActiveForRide(ride.getId());
        List<SeatBooking> confirmed = SeatAllocator.allocate(ride.hasDriver(), ride.getSeatsOffered(), active,
                clock.instant());
        for (SeatBooking booking : confirmed) {
            if (actor != null && booking.getMember().getId().equals(actor.getId())) {
                continue;
            }
            String message = "You have a seat on " + RideMessages.describe(ride) + " with "
                    + RideMessages.bold(ride.getDriver().getDisplayName()) + ".";
            notifications.notify(booking.getMember().getUser(), NotificationType.SEAT_CONFIRMED, message,
                    RideMessages.link(ride));
        }
        return confirmed;
    }
}
