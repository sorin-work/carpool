package com.carpool.cost;

import com.carpool.group.CarpoolGroup;
import com.carpool.group.GroupMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private CarpoolGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_member_id")
    private GroupMember from;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_member_id")
    private GroupMember to;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by")
    private GroupMember recordedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(CarpoolGroup group, GroupMember from, GroupMember to, long amountCents, LocalDate paidOn,
                   GroupMember recordedBy, Instant createdAt) {
        this.group = group;
        this.from = from;
        this.to = to;
        this.amountCents = amountCents;
        this.paidOn = paidOn;
        this.recordedBy = recordedBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public GroupMember getFrom() {
        return from;
    }

    public GroupMember getTo() {
        return to;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }
}
