package com.arogya.cafe.ordering.entity;

import com.arogya.cafe.common.entity.BaseEntity;
import com.arogya.cafe.common.enums.OrderStatus;
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
import java.util.HashSet;
import java.util.Set;

/** A customer's request — the top-level event that drives the KOT and Bill. */
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    /** Staff who handled this order (cashier creates, server delivers). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "order_staff",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "staff_id"))
    private Set<Staff> handledBy = new HashSet<>();

    protected Order() {
    }

    public Order(Customer customer) {
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Set<Staff> getHandledBy() {
        return handledBy;
    }
}
