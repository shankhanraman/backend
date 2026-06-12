package com.arogya.cafe.inventory.entity;

import com.arogya.cafe.catalog.entity.Ingredient;
import com.arogya.cafe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

/** The live running balance of an ingredient currently on hand. One row per ingredient. */
@Entity
@Table(name = "inventory_stock")
public class InventoryStock extends BaseEntity {

    /**
     * Optimistic lock: two concurrent KOT-prepare deductions on the same ingredient can't both
     * commit — the second commit fails with an OptimisticLockException instead of silently
     * overwriting the first and losing a deduction. Guards stock-math correctness under concurrency.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false, unique = true)
    private Ingredient ingredient;

    @Column(name = "qty_on_hand", nullable = false, precision = 14, scale = 3)
    private BigDecimal qtyOnHand;

    @Column(name = "reorder_threshold", nullable = false, precision = 14, scale = 3)
    private BigDecimal reorderThreshold;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated = Instant.now();

    protected InventoryStock() {}

    public InventoryStock(Ingredient ingredient, BigDecimal qtyOnHand, BigDecimal reorderThreshold) {
        this.ingredient = ingredient;
        this.qtyOnHand = qtyOnHand;
        this.reorderThreshold = reorderThreshold;
        this.lastUpdated = Instant.now();
    }

    @Transient
    public boolean isLow() {
        return qtyOnHand.compareTo(reorderThreshold) <= 0;
    }

    public long getVersion() {
        return version;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public BigDecimal getQtyOnHand() {
        return qtyOnHand;
    }

    public void setQtyOnHand(BigDecimal qtyOnHand) {
        this.qtyOnHand = qtyOnHand;
    }

    public BigDecimal getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(BigDecimal reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
