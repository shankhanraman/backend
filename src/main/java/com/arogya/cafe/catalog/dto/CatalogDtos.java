package com.arogya.cafe.catalog.dto;

import com.arogya.cafe.catalog.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Request/response records for the catalog domain. */
public final class CatalogDtos {

    private CatalogDtos() {}

    // ---- Category ----
    public record CategoryRequest(@NotBlank String name) {}

    public record CategoryResponse(Long id, String name) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName());
        }
    }

    // ---- MenuItem ----
    public record MenuItemRequest(
            @NotBlank String name,
            @NotBlank String sizeVariant,
            @NotNull @Positive BigDecimal price,
            @NotNull Long categoryId) {}

    public record MenuItemResponse(
            Long id, String name, String sizeVariant, BigDecimal price, Long categoryId, String categoryName) {
        public static MenuItemResponse from(MenuItem m) {
            return new MenuItemResponse(
                    m.getId(),
                    m.getName(),
                    m.getSizeVariant(),
                    m.getPrice(),
                    m.getCategory().getId(),
                    m.getCategory().getName());
        }
    }

    // ---- Ingredient ----
    public record IngredientRequest(@NotBlank String name, @NotBlank String unit) {}

    public record IngredientResponse(Long id, String name, String unit) {
        public static IngredientResponse from(Ingredient i) {
            return new IngredientResponse(i.getId(), i.getName(), i.getUnit());
        }
    }

    // ---- ItemIngredient (recipe line) ----
    public record ItemIngredientRequest(
            @NotNull Long ingredientId,
            @NotNull @PositiveOrZero BigDecimal quantity,
            @NotBlank String unit,
            String sizeVariant) {}

    public record ItemIngredientResponse(
            Long id,
            Long menuItemId,
            Long ingredientId,
            String ingredientName,
            BigDecimal quantity,
            String unit,
            String sizeVariant) {
        public static ItemIngredientResponse from(ItemIngredient ii) {
            return new ItemIngredientResponse(
                    ii.getId(),
                    ii.getMenuItem().getId(),
                    ii.getIngredient().getId(),
                    ii.getIngredient().getName(),
                    ii.getQuantity(),
                    ii.getUnit(),
                    ii.getSizeVariant());
        }
    }
}
