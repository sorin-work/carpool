package com.carpool.ride;

import com.carpool.group.GroupMember;
import com.carpool.trip.Trip;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** A single run of a trip on one date. */
@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "ride_date", nullable = false)
    private LocalDate rideDate;

    @Column(name = "departure_at", nullable = false)
    private Instant departureAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private GroupMember driver;

    @Column(name = "seats_offered", nullable = false)
    private int seatsOffered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RideStatus status = RideStatus.SCHEDULED;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    /** Something the driver wants riders to know, e.g. "leaving from the back gate". */
    @Column(length = 300)
    private String note;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Ride() {
    }

    public Ride(Trip trip, LocalDate rideDate, Instant departureAt, Instant createdAt) {
        this.trip = trip;
        this.rideDate = rideDate;
        this.departureAt = departureAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public LocalDate getRideDate() {
        return rideDate;
    }

    public Instant getDepartureAt() {
        return departureAt;
    }

    /** A ride at a new time needs a new reminder. */
    public void moveTo(Instant departureAt) {
        if (!departureAt.equals(this.departureAt)) {
            this.departureAt = departureAt;
            this.reminderSentAt = null;
        }
    }

    public GroupMember getDriver() {
        return driver;
    }

    public boolean hasDriver() {
        return driver != null;
    }

    public boolean isDrivenBy(GroupMember member) {
        return driver != null && driver.getId().equals(member.getId());
    }

    public void assignDriver(GroupMember driver, int seats) {
        this.driver = driver;
        this.seatsOffered = seats;
    }

    public void clearDriver() {
        this.driver = null;
        this.seatsOffered = 0;
    }

    public int getSeatsOffered() {
        return seatsOffered;
    }

    public RideStatus getStatus() {
        return status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void cancel(String reason) {
        this.status = RideStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public void reinstate() {
        this.status = RideStatus.SCHEDULED;
        this.cancelReason = null;
    }

    public boolean isCancelled() {
        return status == RideStatus.CANCELLED;
    }

    public boolean hasDeparted(Instant now) {
        return !departureAt.isAfter(now);
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public void markReminded(Instant now) {
        this.reminderSentAt = now;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
