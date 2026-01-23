package com.carpool.swap;

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

/** A driver asking another member to take over a ride, optionally taking one of theirs in return. */
@Entity
@Table(name = "swap_requests")
public class SwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private GroupMember requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id")
    private GroupMember target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_ride_id")
    private Ride offeredRide;

    @Column(length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SwapStatus status = SwapStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected SwapRequest() {
    }

    public SwapRequest(Ride ride, GroupMember requester, GroupMember target, Ride offeredRide, String message,
                       Instant createdAt) {
        this.ride = ride;
        this.requester = requester;
        this.target = target;
        this.offeredRide = offeredRide;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Ride getRide() {
        return ride;
    }

    public GroupMember getRequester() {
        return requester;
    }

    public GroupMember getTarget() {
        return target;
    }

    public Ride getOfferedRide() {
        return offeredRide;
    }

    public String getMessage() {
        return message;
    }

    public SwapStatus getStatus() {
        return status;
    }

    public boolean isPending() {
        return status == SwapStatus.PENDING;
    }

    public void resolve(SwapStatus status, Instant now) {
        this.status = status;
        this.resolvedAt = now;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
