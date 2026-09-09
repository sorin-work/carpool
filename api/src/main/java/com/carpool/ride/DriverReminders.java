package com.carpool.ride;

import com.carpool.booking.BookingStatus;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.notification.NotificationService;
import com.carpool.notification.NotificationType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Reminds drivers about rides in the next day, once per ride. */
@Component
public class DriverReminders {

    static final Duration AHEAD = Duration.ofHours(24);

    private final RideRepository rides;
    private final SeatBookingRepository bookings;
    private final NotificationService notifications;
    private final Clock clock;

    public DriverReminders(RideRepository rides, SeatBookingRepository bookings, NotificationService notifications,
                           Clock clock) {
        this.rides = rides;
        this.bookings = bookings;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.driver-reminder-cron:0 0 * * * *}")
    @Transactional
    public int send() {
        Instant now = clock.instant();
        int sent = 0;
        for (Ride ride : rides.findNeedingReminder(now, now.plus(AHEAD))) {
            long riders = bookings.findActiveForRide(ride.getId()).stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
            String who = riders == 0 ? "nobody has booked a seat yet" : riders == 1 ? "1 rider" : riders + " riders";
            notifications.notify(ride.getDriver().getUser(), NotificationType.DRIVER_REMINDER,
                    "Reminder: you drive " + RideMessages.describe(ride) + " (" + who + ").", RideMessages.link(ride));
            ride.markReminded(now);
            sent++;
        }
        return sent;
    }
}
