package com.arogya.cafe.catalog.entity;

import com.arogya.cafe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Recipe line: how much of an ingredient a menu item uses per serving.
 * sizeVariant lets the recipe differ per size (null = applies to every size of the item).
 */
@Entity
@Table(name = "item_ingredient")
public class ItemIngredient extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    /** Recipe unit (validated against the ingredient's base unit). */
    @Column(nullable = false)
    private String unit;

    @Column(name = "size_variant")
    private String sizeVariant;

    protected ItemIngredient() {}

    public ItemIngredient(
            MenuItem menuItem, Ingredient ingredient, BigDecimal quantity, String unit, String sizeVariant) {
        this.menuItem = menuItem;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = unit;
        this.sizeVariant = sizeVariant;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSizeVariant() {
        return sizeVariant;
    }

    public void setSizeVariant(String sizeVariant) {
        this.sizeVariant = sizeVariant;
    }
}
