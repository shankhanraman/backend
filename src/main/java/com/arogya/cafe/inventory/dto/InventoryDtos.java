package com.arogya.cafe.inventory.dto;
import com.arogya.cafe.inventory.entity.*;

import com.arogya.cafe.common.enums.StockTransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

/** Request/response records for the inventory domain. */
public final class InventoryDtos {

    private InventoryDtos() {
    }

    // ---- Supplier ----
    public record SupplierRequest(@NotBlank String name, @NotBlank String contact) {
    }

    public record SupplierResponse(Long id, String name, String contact) {
        public static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.getId(), s.getName(), s.getContact());
        }
    }

    // ---- Inventory stock ----
    public record CreateStockRequest(
            @NotNull Long ingredientId,
            @NotNull @PositiveOrZero BigDecimal qtyOnHand,
            @NotNull @PositiveOrZero BigDecimal reorderThreshold) {
    }

    public record StockResponse(
            Long id, Long ingredientId, String ingredientName, String unit,
            BigDecimal qtyOnHand, BigDecimal reorderThreshold, boolean low, Instant lastUpdated) {
        public static StockResponse from(InventoryStock s) {
            return new StockResponse(s.getId(), s.getIngredient().getId(), s.getIngredient().getName(),
                    s.getIngredient().getUnit(), s.getQtyOnHand(), s.getReorderThreshold(), s.isLow(),
                    s.getLastUpdated());
        }
    }

    public record RestockRequest(@NotNull @Positive BigDecimal quantity, Long supplierId) {
    }

    // ---- Stock transaction ----
    public record StockTransactionResponse(
            Long id, Long ingredientId, String ingredientName, StockTransactionType type,
            BigDecimal quantity, String triggeredBy, Long supplierId, Long orderId, Instant createdAt) {
        public static StockTransactionResponse from(StockTransaction t) {
            return new StockTransactionResponse(
                    t.getId(), t.getInventoryStock().getIngredient().getId(),
                    t.getInventoryStock().getIngredient().getName(), t.getType(), t.getQuantity(),
                    t.getTriggeredBy(), t.getSupplier() != null ? t.getSupplier().getId() : null,
                    t.getOrderId(), t.getCreatedAt());
        }
    }
}
