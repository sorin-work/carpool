package com.carpool.ride;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    @Query("select r.rideDate from Ride r where r.trip.id = :tripId and r.rideDate >= :from and r.rideDate < :to")
    List<LocalDate> findDatesForTrip(Long tripId, LocalDate from, LocalDate to);

    @Query("""
            select r from Ride r
            join fetch r.trip t
            left join fetch r.driver d
            left join fetch d.user
            where t.group.id = :groupId and r.rideDate >= :from and r.rideDate < :to
            order by r.departureAt, t.name
            """)
    List<Ride> findForGroup(Long groupId, LocalDate from, LocalDate to);

    @Query("""
            select r from Ride r
            join fetch r.trip t
            join fetch t.group
            left join fetch r.driver d
            left join fetch d.user
            where r.id = :id
            """)
    Optional<Ride> findWithDetails(Long id);

    @Query("""
            select new com.carpool.ride.DriverHistory(r.driver.id, count(r), max(r.rideDate))
            from Ride r
            where r.trip.group.id = :groupId and r.driver is not null
              and r.status = com.carpool.ride.RideStatus.SCHEDULED and r.rideDate >= :since
            group by r.driver.id
            """)
    List<DriverHistory> findDriverHistory(Long groupId, LocalDate since);

    /** Rides that actually happened: not cancelled, had a driver and left before {@code now}. */
    @Query("""
            select r from Ride r
            join fetch r.trip t
            join fetch r.driver d
            join fetch d.user
            where t.group.id = :groupId and r.rideDate >= :from and r.rideDate < :to
              and r.status = com.carpool.ride.RideStatus.SCHEDULED and r.departureAt <= :now
            order by r.departureAt
            """)
    List<Ride> findCompleted(Long groupId, LocalDate from, LocalDate to, Instant now);

    @Query("""
            select count(r) from Ride r
            where r.trip.group.id = :groupId and r.rideDate >= :from and r.rideDate < :to
              and r.status = com.carpool.ride.RideStatus.CANCELLED and r.departureAt <= :now
            """)
    int countCancelled(Long groupId, LocalDate from, LocalDate to, Instant now);

    @Query("select r from Ride r join fetch r.trip t join fetch t.group where r.driver.id = :memberId and r.departureAt > :now")
    List<Ride> findUpcomingDrivenBy(Long memberId, Instant now);

    @Query("select r from Ride r join fetch r.trip t join fetch t.group where t.id = :tripId and r.departureAt > :now")
    List<Ride> findUpcomingForTrip(Long tripId, Instant now);
}
