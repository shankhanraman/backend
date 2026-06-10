package com.arogya.cafe.catalog.entity;

import com.arogya.cafe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Any product a customer can order; carries a size variant and price. */
@Entity
@Table(name = "menu_item")
public class MenuItem extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "size_variant", nullable = false)
    private String sizeVariant;

    /** Added beyond the ERD: required to compute a bill total. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected MenuItem() {
    }

    public MenuItem(String name, String sizeVariant, BigDecimal price, Category category) {
        this.name = name;
        this.sizeVariant = sizeVariant;
        this.price = price;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSizeVariant() {
        return sizeVariant;
    }

    public void setSizeVariant(String sizeVariant) {
        this.sizeVariant = sizeVariant;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
