package com.arogya.cafe.catalog.repository;
import com.arogya.cafe.catalog.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
