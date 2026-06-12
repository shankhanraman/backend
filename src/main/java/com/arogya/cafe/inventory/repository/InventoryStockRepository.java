package com.arogya.cafe.inventory.repository;

import com.arogya.cafe.inventory.entity.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {

    InventoryStock findByIngredientId(Long ingredientId);

    @Query("select s from InventoryStock s where s.qtyOnHand <= s.reorderThreshold")
    List<InventoryStock> findLowStock();
}
