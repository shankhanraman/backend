package com.arogya.cafe.catalog.entity;

import com.arogya.cafe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A raw material used to make items — the master stockroom list. */
@Entity
@Table(name = "ingredient")
public class Ingredient extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    /** Base unit of measure (ml, g, pcs...). */
    @Column(nullable = false)
    private String unit;

    protected Ingredient() {}

    public Ingredient(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
