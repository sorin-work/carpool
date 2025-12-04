package com.carpool.group;

import com.carpool.user.User;
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
@Table(name = "group_members")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private CarpoolGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MemberRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "can_drive", nullable = false)
    private boolean canDrive;

    /** Passenger seats the member's car has, not counting the driver. */
    @Column(name = "car_seats", nullable = false)
    private int carSeats;

    @Column(name = "pickup_note", length = 200)
    private String pickupNote;

    protected GroupMember() {
    }

    public GroupMember(CarpoolGroup group, User user, MemberRole role, Instant joinedAt) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public CarpoolGroup getGroup() {
        return group;
    }

    public User getUser() {
        return user;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public boolean isCanDrive() {
        return canDrive;
    }

    public int getCarSeats() {
        return carSeats;
    }

    public void setDriving(boolean canDrive, int carSeats) {
        this.canDrive = canDrive;
        this.carSeats = canDrive ? carSeats : 0;
    }

    public String getPickupNote() {
        return pickupNote;
    }

    public void setPickupNote(String pickupNote) {
        this.pickupNote = pickupNote;
    }

    public String getDisplayName() {
        return user.getDisplayName();
    }
}
