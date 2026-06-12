package com.arogya.cafe.ordering.controller;

import com.arogya.cafe.ordering.dto.OrderingDtos.CreateOrderRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.OrderResponse;
import com.arogya.cafe.ordering.service.*;
import com.arogya.cafe.security.entity.*;
import com.arogya.cafe.security.provider.CurrentStaffProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentStaffProvider currentStaff;

    public OrderController(OrderService orderService, CurrentStaffProvider currentStaff) {
        this.orderService = orderService;
        this.currentStaff = currentStaff;
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orderService.listOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    /** Only the cashier/manager opens an order (creating its Bill). Role gated in SecurityConfig. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.createOrder(req, currentStaff.require());
    }

    /** Only the server/manager marks an order served (after the KOT is prepared). Role gated in SecurityConfig. */
    @PostMapping("/{id}/serve")
    public OrderResponse serve(@PathVariable Long id) {
        return orderService.markServed(id, currentStaff.require());
    }
}
