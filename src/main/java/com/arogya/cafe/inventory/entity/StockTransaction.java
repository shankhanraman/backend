package com.arogya.cafe.inventory.entity;

import com.arogya.cafe.common.entity.BaseEntity;
import com.arogya.cafe.common.enums.StockTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** One recorded stock movement — consumed (order) or restocked (supplier delivery). */
@Entity
@Table(name = "stock_transaction")
public class StockTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inventory_stock_id", nullable = false)
    private InventoryStock inventoryStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockTransactionType type;

    /** Signed-as-positive amount moved; direction is given by type. */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    /** Free-text origin of the movement (e.g. "ORDER #101", "Sharma Dairy delivery"). */
    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Order that caused a CONSUMED movement, if any. */
    @Column(name = "order_id")
    private Long orderId;

    protected StockTransaction() {}

    public StockTransaction(
            InventoryStock inventoryStock,
            StockTransactionType type,
            BigDecimal quantity,
            String triggeredBy,
            Supplier supplier,
            Long orderId) {
        this.inventoryStock = inventoryStock;
        this.type = type;
        this.quantity = quantity;
        this.triggeredBy = triggeredBy;
        this.supplier = supplier;
        this.orderId = orderId;
    }

    public InventoryStock getInventoryStock() {
        return inventoryStock;
    }

    public StockTransactionType getType() {
        return type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public Long getOrderId() {
        return orderId;
    }
}
