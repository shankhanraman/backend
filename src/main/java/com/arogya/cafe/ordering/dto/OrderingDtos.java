package com.arogya.cafe.ordering.dto;

import com.arogya.cafe.common.enums.KotStatus;
import com.arogya.cafe.common.enums.OrderStatus;
import com.arogya.cafe.common.enums.PaymentStatus;
import com.arogya.cafe.ordering.entity.*;
import com.arogya.cafe.security.entity.Staff;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request/response records for the ordering domain. */
public final class OrderingDtos {

    private OrderingDtos() {}

    // ---- Customer ----
    public record CustomerRequest(@NotBlank String name, @NotBlank String contact) {}

    public record CustomerResponse(Long id, String name, String contact) {
        public static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.getId(), c.getName(), c.getContact());
        }
    }

    // ---- Order creation ----
    public record OrderLineRequest(
            @NotNull Long menuItemId, @NotBlank String sizeVariant, @NotNull @Positive Integer quantity) {}

    public record CreateOrderRequest(
            @NotNull Long customerId, @NotEmpty @Valid List<OrderLineRequest> lines, @NotBlank String paymentMethod) {
        // paymentMethod: "cash", "card", "upi", etc.
    }

    // ---- Responses ----
    public record OrderLineResponse(
            Long id,
            Long menuItemId,
            String menuItemName,
            String sizeVariant,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
        public static OrderLineResponse from(OrderLine l) {
            return new OrderLineResponse(
                    l.getId(),
                    l.getMenuItem().getId(),
                    l.getMenuItem().getName(),
                    l.getSizeVariant(),
                    l.getQuantity(),
                    l.getUnitPrice(),
                    l.getLineTotal());
        }
    }

    public record KotResponse(Long id, Long orderId, KotStatus status, Instant issuedAt, List<String> fulfilledBy) {
        public static KotResponse from(Kot k) {
            return new KotResponse(
                    k.getId(),
                    k.getOrder().getId(),
                    k.getStatus(),
                    k.getIssuedAt(),
                    k.getFulfilledBy().stream().map(Staff::getName).toList());
        }
    }

    public record BillResponse(
            Long id, Long orderId, BigDecimal totalAmount, PaymentStatus paymentStatus, Instant billedAt) {
        public static BillResponse from(Bill b) {
            return new BillResponse(
                    b.getId(), b.getOrder().getId(), b.getTotalAmount(), b.getPaymentStatus(), b.getBilledAt());
        }
    }

    public record OrderResponse(
            Long id,
            OrderStatus status,
            Instant createdAt,
            Long customerId,
            String customerName,
            List<String> handledBy,
            List<OrderLineResponse> lines,
            KotResponse kot,
            BillResponse bill) {

        public static OrderResponse build(Order order, List<OrderLine> lines, Kot kot, Bill bill) {
            return new OrderResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getCreatedAt(),
                    order.getCustomer().getId(),
                    order.getCustomer().getName(),
                    order.getHandledBy().stream().map(Staff::getName).toList(),
                    lines.stream().map(OrderLineResponse::from).toList(),
                    KotResponse.from(kot),
                    BillResponse.from(bill));
        }
    }
}
