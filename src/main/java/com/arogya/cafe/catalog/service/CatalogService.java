package com.arogya.cafe.catalog.service;

import com.arogya.cafe.catalog.dto.CatalogDtos.CategoryRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.IngredientRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.ItemIngredientRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.MenuItemRequest;
import com.arogya.cafe.catalog.entity.*;
import com.arogya.cafe.catalog.repository.*;
import com.arogya.cafe.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {

    private final CategoryRepository categories;
    private final MenuItemRepository menuItems;
    private final IngredientRepository ingredients;
    private final ItemIngredientRepository itemIngredients;

    public CatalogService(
            CategoryRepository categories,
            MenuItemRepository menuItems,
            IngredientRepository ingredients,
            ItemIngredientRepository itemIngredients) {
        this.categories = categories;
        this.menuItems = menuItems;
        this.ingredients = ingredients;
        this.itemIngredients = itemIngredients;
    }

    // ---- Category ----
    public Category createCategory(CategoryRequest req) {
        return categories.save(new Category(req.name()));
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categories.findAll();
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
    }

    public Category updateCategory(Long id, CategoryRequest req) {
        Category category = getCategory(id);
        category.setName(req.name());
        return category;
    }

    public void deleteCategory(Long id) {
        categories.deleteById(id);
    }

    // ---- MenuItem ----
    public MenuItem createMenuItem(MenuItemRequest req) {
        Category category = getCategory(req.categoryId());
        return menuItems.save(new MenuItem(req.name(), req.sizeVariant(), req.price(), category));
    }

    @Transactional(readOnly = true)
    public List<MenuItem> listMenuItems() {
        return menuItems.findAll();
    }

    @Transactional(readOnly = true)
    public MenuItem getMenuItem(Long id) {
        return menuItems.findById(id).orElseThrow(() -> new NotFoundException("MenuItem " + id + " not found"));
    }

    public MenuItem updateMenuItem(Long id, MenuItemRequest req) {
        Category category = getCategory(req.categoryId());
        MenuItem item = getMenuItem(id);
        item.setName(req.name());
        item.setSizeVariant(req.sizeVariant());
        item.setPrice(req.price());
        item.setCategory(category);
        return item;
    }

    public void deleteMenuItem(Long id) {
        menuItems.deleteById(id);
    }

    // ---- Ingredient ----
    public Ingredient createIngredient(IngredientRequest req) {
        return ingredients.save(new Ingredient(req.name(), req.unit()));
    }

    @Transactional(readOnly = true)
    public List<Ingredient> listIngredients() {
        return ingredients.findAll();
    }

    @Transactional(readOnly = true)
    public Ingredient getIngredient(Long id) {
        return ingredients.findById(id).orElseThrow(() -> new NotFoundException("Ingredient " + id + " not found"));
    }

    public Ingredient updateIngredient(Long id, IngredientRequest req) {
        Ingredient ingredient = getIngredient(id);
        ingredient.setName(req.name());
        ingredient.setUnit(req.unit());
        return ingredient;
    }

    public void deleteIngredient(Long id) {
        ingredients.deleteById(id);
    }

    // ---- ItemIngredient (recipe) ----
    public ItemIngredient addRecipeLine(Long menuItemId, ItemIngredientRequest req) {
        MenuItem menuItem = getMenuItem(menuItemId);
        Ingredient ingredient = getIngredient(req.ingredientId());
        return itemIngredients.save(
                new ItemIngredient(menuItem, ingredient, req.quantity(), req.unit(), req.sizeVariant()));
    }

    @Transactional(readOnly = true)
    public List<ItemIngredient> listRecipe(Long menuItemId) {
        getMenuItem(menuItemId); // validates existence
        return itemIngredients.findByMenuItemId(menuItemId);
    }

    public void deleteRecipeLine(Long id) {
        itemIngredients.deleteById(id);
    }
}
