package com.arogya.cafe.catalog.controller;
import com.arogya.cafe.catalog.entity.*;
import com.arogya.cafe.catalog.repository.*;
import com.arogya.cafe.catalog.dto.*;
import com.arogya.cafe.catalog.service.*;
import com.arogya.cafe.catalog.service.*;

import com.arogya.cafe.catalog.dto.CatalogDtos.IngredientRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.IngredientResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final CatalogService service;

    public IngredientController(CatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<IngredientResponse> list() {
        return service.listIngredients().stream().map(IngredientResponse::from).toList();
    }

    @GetMapping("/{id}")
    public IngredientResponse get(@PathVariable Long id) {
        return IngredientResponse.from(service.getIngredient(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse create(@Valid @RequestBody IngredientRequest req) {
        return IngredientResponse.from(service.createIngredient(req));
    }

    @PutMapping("/{id}")
    public IngredientResponse update(@PathVariable Long id, @Valid @RequestBody IngredientRequest req) {
        return IngredientResponse.from(service.updateIngredient(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteIngredient(id);
        return ResponseEntity.noContent().build();
    }
}
