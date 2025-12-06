package com.carpool.trip;

import com.carpool.group.CarpoolGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** A recurring journey, e.g. "Mon-Fri 07:45 from Elm St to Riverside Primary". */
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private CarpoolGroup group;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 200)
    private String origin;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "days_of_week", nullable = false)
    private int daysOfWeek;

    @Column(name = "distance_km", nullable = false, precision = 7, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Trip() {
    }

    public Trip(CarpoolGroup group, String name, String origin, String destination, LocalTime departureTime,
                Set<DayOfWeek> days, BigDecimal distanceKm, LocalDate startsOn, LocalDate endsOn, Instant createdAt) {
        this.group = group;
        this.name = name;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.daysOfWeek = DaysOfWeek.toMask(days);
        this.distanceKm = distanceKm;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public CarpoolGroup getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public Set<DayOfWeek> getDays() {
        return DaysOfWeek.fromMask(daysOfWeek);
    }

    public void setDays(Set<DayOfWeek> days) {
        this.daysOfWeek = DaysOfWeek.toMask(days);
    }

    public boolean runsOn(DayOfWeek day) {
        return DaysOfWeek.includes(daysOfWeek, day);
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public void setStartsOn(LocalDate startsOn) {
        this.startsOn = startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public void setEndsOn(LocalDate endsOn) {
        this.endsOn = endsOn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
