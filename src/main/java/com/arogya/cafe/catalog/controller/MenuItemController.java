package com.arogya.cafe.catalog.controller;

import com.arogya.cafe.catalog.dto.*;
import com.arogya.cafe.catalog.dto.CatalogDtos.ItemIngredientRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.ItemIngredientResponse;
import com.arogya.cafe.catalog.dto.CatalogDtos.MenuItemRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.MenuItemResponse;
import com.arogya.cafe.catalog.entity.*;
import com.arogya.cafe.catalog.repository.*;
import com.arogya.cafe.catalog.service.*;
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
@RequestMapping("/api/menu-items")
public class MenuItemController {

    private final CatalogService service;

    public MenuItemController(CatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<MenuItemResponse> list() {
        return service.listMenuItems().stream().map(MenuItemResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MenuItemResponse get(@PathVariable Long id) {
        return MenuItemResponse.from(service.getMenuItem(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse create(@Valid @RequestBody MenuItemRequest req) {
        return MenuItemResponse.from(service.createMenuItem(req));
    }

    @PutMapping("/{id}")
    public MenuItemResponse update(@PathVariable Long id, @Valid @RequestBody MenuItemRequest req) {
        return MenuItemResponse.from(service.updateMenuItem(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Recipe (item-ingredient) sub-resource ----
    @GetMapping("/{id}/ingredients")
    public List<ItemIngredientResponse> recipe(@PathVariable Long id) {
        return service.listRecipe(id).stream().map(ItemIngredientResponse::from).toList();
    }

    @PostMapping("/{id}/ingredients")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemIngredientResponse addRecipeLine(@PathVariable Long id, @Valid @RequestBody ItemIngredientRequest req) {
        return ItemIngredientResponse.from(service.addRecipeLine(id, req));
    }

    @DeleteMapping("/{id}/ingredients/{lineId}")
    public ResponseEntity<Void> deleteRecipeLine(@PathVariable Long id, @PathVariable Long lineId) {
        service.deleteRecipeLine(lineId);
        return ResponseEntity.noContent().build();
    }
}
