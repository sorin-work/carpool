package com.carpool.booking;

import com.carpool.group.GroupMember;
import com.carpool.ride.Ride;
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

@Entity
@Table(name = "seat_bookings")
public class SeatBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private GroupMember member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SeatBooking() {
    }

    public SeatBooking(Ride ride, GroupMember member, Instant now) {
        this.ride = ride;
        this.member = member;
        this.status = BookingStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Ride getRide() {
        return ride;
    }

    public GroupMember getMember() {
        return member;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status, Instant now) {
        if (this.status != status) {
            this.status = status;
            this.updatedAt = now;
        }
    }

    /** Asking again after cancelling puts the rider at the back of the queue. */
    public void reopen(Instant now) {
        this.status = BookingStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
