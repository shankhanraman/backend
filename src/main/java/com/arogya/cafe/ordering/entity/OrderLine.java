package com.arogya.cafe.ordering.entity;

import com.arogya.cafe.catalog.entity.MenuItem;
import com.arogya.cafe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;

/** One item within an order: which menu item, which size, how many, at what unit price. */
@Entity
@Table(name = "order_line")
public class OrderLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "size_variant", nullable = false)
    private String sizeVariant;

    @Column(nullable = false)
    private int quantity;

    /** Price snapshot taken at order time. */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    protected OrderLine() {}

    public OrderLine(Order order, MenuItem menuItem, String sizeVariant, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.menuItem = menuItem;
        this.sizeVariant = sizeVariant;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    @Transient
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Order getOrder() {
        return order;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public String getSizeVariant() {
        return sizeVariant;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
