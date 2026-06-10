package com.arogya.cafe.ordering.entity;

import com.arogya.cafe.common.entity.BaseEntity;
import com.arogya.cafe.common.enums.KotStatus;
import com.arogya.cafe.security.entity.Staff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** Kitchen Order Ticket — the slip sent to the chef. One per order. */
@Entity
@Table(name = "kot")
public class Kot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KotStatus status = KotStatus.PENDING;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "kot_staff",
            joinColumns = @JoinColumn(name = "kot_id"),
            inverseJoinColumns = @JoinColumn(name = "staff_id"))
    private Set<Staff> fulfilledBy = new HashSet<>();

    protected Kot() {
    }

    public Kot(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public KotStatus getStatus() {
        return status;
    }

    public void setStatus(KotStatus status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Set<Staff> getFulfilledBy() {
        return fulfilledBy;
    }
}
