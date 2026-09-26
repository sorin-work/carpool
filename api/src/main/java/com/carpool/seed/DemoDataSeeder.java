package com.carpool.seed;

import com.carpool.booking.BookingStatus;
import com.carpool.booking.SeatBooking;
import com.carpool.booking.SeatBookingRepository;
import com.carpool.group.CarpoolGroup;
import com.carpool.group.GroupMember;
import com.carpool.group.GroupMemberRepository;
import com.carpool.group.GroupRepository;
import com.carpool.group.MemberRole;
import com.carpool.notification.NotificationService;
import com.carpool.notification.NotificationType;
import com.carpool.ride.Ride;
import com.carpool.ride.RideRepository;
import com.carpool.ride.RideService;
import com.carpool.rotation.Absence;
import com.carpool.rotation.AbsenceRepository;
import com.carpool.swap.SwapRepository;
import com.carpool.swap.SwapRequest;
import com.carpool.trip.Trip;
import com.carpool.trip.TripRepository;
import com.carpool.user.User;
import com.carpool.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Creates a demo account with two groups, a few months of ride history and upcoming rides, so the
 * app has something to show on first start. Runs only with {@code app.seed-demo=true} and only once.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@carpool.local";
    public static final String DEMO_PASSWORD = "demo-password";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");

    private final UserRepository users;
    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final TripRepository trips;
    private final RideRepository rides;
    private final RideService rideService;
    private final SeatBookingRepository bookings;
    private final AbsenceRepository absences;
    private final NotificationService notifications;
    private final SwapRepository swaps;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataSeeder(UserRepository users, GroupRepository groups, GroupMemberRepository members,
                          TripRepository trips, RideRepository rides, RideService rideService,
                          SeatBookingRepository bookings, AbsenceRepository absences,
                          NotificationService notifications, SwapRepository swaps,
                          PasswordEncoder passwordEncoder, Clock clock) {
        this.swaps = swaps;
        this.users = users;
        this.groups = groups;
        this.members = members;
        this.trips = trips;
        this.rides = rides;
        this.rideService = rideService;
        this.bookings = bookings;
        this.absences = absences;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        Random random = new Random(4419);
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        User demo = user(DEMO_EMAIL, "Dana Demo", hash);
        User mihai = user("mihai@carpool.local", "Mihai Ionescu", hash);
        User elena = user("elena@carpool.local", "Elena Popa", hash);
        User radu = user("radu@carpool.local", "Radu Stan", hash);
        User ioana = user("ioana@carpool.local", "Ioana Marin", hash);
        User andrei = user("andrei@carpool.local", "Andrei Dobre", hash);

        // A school run with four parents who take turns driving.
        CarpoolGroup school = group("Elm St school run", "Weekday mornings to Riverside Primary", "SCHRUN26",
                new BigDecimal("0.180"));
        GroupMember dana = member(school, demo, MemberRole.OWNER, 3, "12 Elm St, blue gate");
        GroupMember m1 = member(school, mihai, MemberRole.ADMIN, 3, null);
        GroupMember m2 = member(school, elena, MemberRole.MEMBER, 4, "Corner of Elm St and 3rd");
        GroupMember m3 = member(school, radu, MemberRole.MEMBER, 2, null);
        GroupMember m4 = member(school, ioana, MemberRole.MEMBER, 0, "Bakery on Oak Ave");
        List<GroupMember> schoolDrivers = List.of(dana, m1, m2, m3);
        List<GroupMember> schoolAll = List.of(dana, m1, m2, m3, m4);

        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        LocalDate historyStart = today.minusWeeks(16).with(DayOfWeek.MONDAY);
        Trip morning = trips.save(new Trip(school, "Morning school run", "12 Elm St", "Riverside Primary",
                LocalTime.of(7, 45), EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), new BigDecimal("6.40"),
                historyStart, null, clock.instant()));
        trips.save(new Trip(school, "Friday pickup", "Riverside Primary", "12 Elm St", LocalTime.of(15, 30),
                EnumSet.of(DayOfWeek.FRIDAY), new BigDecimal("6.40"), historyStart, null, clock.instant()));

        // An office pool with a longer commute.
        CarpoolGroup office = group("Northside office pool", "Tue and Thu to the Northside campus", "NRTHPQ27",
                new BigDecimal("0.210"));
        GroupMember o1 = member(office, demo, MemberRole.MEMBER, 3, null);
        GroupMember o2 = member(office, andrei, MemberRole.OWNER, 4, null);
        GroupMember o3 = member(office, elena, MemberRole.MEMBER, 0, null);
        trips.save(new Trip(office, "Commute to Northside", "Piata Victoriei", "Northside campus",
                LocalTime.of(8, 15), EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), new BigDecimal("18.50"),
                historyStart, null, clock.instant()));

        rideService.materialize(school, historyStart, today.plusDays(21));
        rideService.materialize(office, historyStart, today.plusDays(21));

        fill(school, schoolDrivers, schoolAll, historyStart, today, random);
        fill(office, List.of(o1, o2), List.of(o1, o2, o3), historyStart, today, random);

        absences.save(new Absence(m2, today.plusDays(8), today.plusDays(12), "Family holiday", clock.instant()));

        // Mihai asks Dana to take one of his upcoming rides, so the swaps page has something to answer.
        rides.findForGroup(school.getId(), today.plusDays(1), today.plusDays(4)).stream()
                .filter(r -> r.isDrivenBy(m1) && !r.isCancelled())
                .findFirst()
                .ifPresent(r -> {
                    swaps.save(new SwapRequest(r, m1, dana, null, "Work trip that morning, could you take it?",
                            clock.instant()));
                    notifications.notify(demo, NotificationType.SWAP_REQUESTED,
                            "**Mihai Ionescu** asks if you can drive **" + r.getTrip().getName() + "**.",
                            "/groups/" + school.getId() + "/swaps");
                });

        notifications.notify(demo, NotificationType.SEAT_CONFIRMED,
                "You have a seat on **" + morning.getName() + "** tomorrow with **Mihai Ionescu**.",
                "/groups/" + school.getId());
        notifications.notify(demo, NotificationType.RIDE_CANCELLED,
                "**Commute to Northside** last Thursday was cancelled by **Andrei Dobre**: Office closed for a power cut.",
                "/groups/" + office.getId());
        log.info("Demo data created: sign in as {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    /** Past rides get drivers and riders round-robin; the next week is left partly open for the rotation. */
    private void fill(CarpoolGroup group, List<GroupMember> drivers, List<GroupMember> everyone, LocalDate from,
                      LocalDate today, Random random) {
        int turn = 0;
        for (Ride ride : rides.findForGroup(group.getId(), from, today.plusDays(21))) {
            boolean past = ride.hasDeparted(clock.instant());
            if (!past && ride.getRideDate().isAfter(today.plusDays(3))) {
                continue;
            }
            if (past && random.nextInt(25) == 0) {
                ride.cancel(random.nextBoolean() ? "School closed" : "Driver is ill");
                continue;
            }
            GroupMember driver = drivers.get(turn++ % drivers.size());
            ride.assignDriver(driver, driver.getCarSeats());
            int seats = driver.getCarSeats();
            for (GroupMember rider : everyone) {
                if (rider == driver || seats == 0 || random.nextInt(4) == 0) {
                    continue;
                }
                SeatBooking booking = new SeatBooking(ride, rider, ride.getCreatedAt());
                booking.setStatus(BookingStatus.CONFIRMED, ride.getCreatedAt());
                bookings.save(booking);
                seats--;
            }
        }
    }

    private User user(String email, String name, String hash) {
        return users.save(new User(email, name, hash, clock.instant()));
    }

    private CarpoolGroup group(String name, String description, String code, BigDecimal costPerKm) {
        CarpoolGroup group = new CarpoolGroup(name, description, code, ZONE, clock.instant());
        group.setCostPerKm(costPerKm);
        return groups.save(group);
    }

    private GroupMember member(CarpoolGroup group, User user, MemberRole role, int seats, String pickupNote) {
        GroupMember member = new GroupMember(group, user, role, clock.instant());
        member.setDriving(seats > 0, seats);
        member.setPickupNote(pickupNote);
        return members.save(member);
    }
}
