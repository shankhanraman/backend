package com.arogya.cafe.catalog.repository;

import com.arogya.cafe.catalog.entity.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemIngredientRepository extends JpaRepository<ItemIngredient, Long> {
    List<ItemIngredient> findByMenuItemId(Long menuItemId);
}
