package com.arogya.cafe.inventory.controller;

import com.arogya.cafe.inventory.dto.InventoryDtos.SupplierRequest;
import com.arogya.cafe.inventory.dto.InventoryDtos.SupplierResponse;
import com.arogya.cafe.inventory.service.*;
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
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final StockService service;

    public SupplierController(StockService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierResponse> list() {
        return service.listSuppliers().stream().map(SupplierResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable Long id) {
        return SupplierResponse.from(service.getSupplier(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest req) {
        return SupplierResponse.from(service.createSupplier(req));
    }
}
