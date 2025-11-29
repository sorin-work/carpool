package com.carpool.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.ZoneId;

@Entity
@Table(name = "carpool_groups")
public class CarpoolGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "invite_code", nullable = false, unique = true, length = 12)
    private String inviteCode;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CarpoolGroup() {
    }

    public CarpoolGroup(String name, String description, String inviteCode, ZoneId timezone, Instant createdAt) {
        this.name = name;
        this.description = description;
        this.inviteCode = inviteCode;
        this.timezone = timezone.getId();
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public ZoneId getZone() {
        return ZoneId.of(timezone);
    }

    public void setZone(ZoneId zone) {
        this.timezone = zone.getId();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
