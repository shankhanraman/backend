package com.arogya.cafe.inventory.controller;

import com.arogya.cafe.inventory.dto.InventoryDtos.CreateStockRequest;
import com.arogya.cafe.inventory.dto.InventoryDtos.RestockRequest;
import com.arogya.cafe.inventory.dto.InventoryDtos.StockResponse;
import com.arogya.cafe.inventory.dto.InventoryDtos.StockTransactionResponse;
import com.arogya.cafe.inventory.service.*;
import jakarta.validation.Valid;
import java.security.Principal;
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
@RequestMapping("/api/inventory")
public class InventoryController {

    private final StockService service;

    public InventoryController(StockService service) {
        this.service = service;
    }

    @GetMapping
    public List<StockResponse> list() {
        return service.listStock().stream().map(StockResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse create(@Valid @RequestBody CreateStockRequest req) {
        return StockResponse.from(service.createStock(req));
    }

    @GetMapping("/low-stock")
    public List<StockResponse> lowStock() {
        return service.lowStock().stream().map(StockResponse::from).toList();
    }

    @GetMapping("/{ingredientId}")
    public StockResponse get(@PathVariable Long ingredientId) {
        return StockResponse.from(service.getStockByIngredient(ingredientId));
    }

    @GetMapping("/{ingredientId}/transactions")
    public List<StockTransactionResponse> transactions(@PathVariable Long ingredientId) {
        return service.transactionsForIngredient(ingredientId).stream()
                .map(StockTransactionResponse::from)
                .toList();
    }

    @PostMapping("/{ingredientId}/restock")
    public StockResponse restock(
            @PathVariable Long ingredientId, @Valid @RequestBody RestockRequest req, Principal principal) {
        String by = "Restock by " + (principal != null ? principal.getName() : "system");
        return StockResponse.from(service.restock(ingredientId, req, by));
    }
}
